/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.InvalidFileNameException;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.file.event.FilePublishedEvent;
import org.obiba.mica.file.event.FileUnPublishedEvent;
import org.obiba.mica.file.event.FileUpdatedEvent;
import org.obiba.mica.file.notification.FilePublicationFlowMailNotification;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.network.event.NetworkUpdatedEvent;
import org.obiba.mica.project.event.ProjectPublishedEvent;
import org.obiba.mica.project.event.ProjectUnpublishedEvent;
import org.obiba.mica.project.event.ProjectUpdatedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import static java.util.stream.Collectors.toList;

@Component
public class FileSystemService {

  private static final Logger log = LoggerFactory.getLogger(FileSystemService.class);

  public static final String DIR_NAME = ".";

  @Inject
  private EventBus eventBus;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private AttachmentStateRepository attachmentStateRepository;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private FilePublicationFlowMailNotification filePublicationFlowNotification;

  @Inject
  private TempFileService tempFileService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  protected SubjectAclService subjectAclService;

  private ReentrantLock fsLock = new ReentrantLock();

  //
  // Persistence
  //

  public void save(Attachment attachment) {
    Attachment saved = attachment;
    validateFileName(attachment.getName());
    List<AttachmentState> states = attachmentStateRepository.findByPathAndName(saved.getPath(), saved.getName());
    AttachmentState state = states.isEmpty() ? new AttachmentState() : states.get(0);

    if(attachment.isNew()) {
      attachment.setId(new ObjectId().toString());
    } else {
      saved = attachmentRepository.findOne(attachment.getId());
      if(saved == null || attachment.isJustUploaded()) {
        saved = attachment;
      } else if(state.isPublished() && state.getPublishedAttachment().getId().equals(attachment.getId())) {
        // about to update a published attachment, so make a soft copy of it
        attachment.setFileReference(saved.getFileReference());
        attachment.setCreatedDate(DateTime.now());
        attachment.setId(new ObjectId().toString());
        saved = attachment;
      } else {
        BeanUtils.copyProperties(attachment, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
            "lastModifiedDate", "fileReference");
      }

      saved.setLastModifiedDate(DateTime.now());
      saved.setLastModifiedBy(getCurrentUsername());
    }

    if(saved.isJustUploaded()) {
      if(attachmentRepository.exists(saved.getId())) {
        // replace already existing attachment
        fileStoreService.delete(saved.getId());
        attachmentRepository.delete(saved.getId());
      }
      fileStoreService.save(saved.getId());
      saved.setJustUploaded(false);
    }

    attachmentRepository.save(saved);

    state.setAttachment(saved);
    state.setLastModifiedDate(DateTime.now());
    state.setLastModifiedBy(getCurrentUsername());
    if(state.isNew()) {
      if(FileUtils.isDirectory(state)) {
        mkdirs(FileUtils.getParentPath(saved.getPath()));
      } else {
        mkdirs(saved.getPath());
      }
    }
    attachmentStateRepository.save(state);

    eventBus.post(new FileUpdatedEvent(state));
  }

  /**
   * Make sure the {@link AttachmentState} is not published and delete it.
   *
   * @param state
   */
  public void delete(AttachmentState state) {
    if(state.isPublished()) publish(state, false);
    attachmentStateRepository.delete(state);
    eventBus.post(new FileDeletedEvent(state));
  }

  /**
   * Delete all unpublished {@link AttachmentState}s corresponding to the given path.
   *
   * @param path
   */
  public void delete(String path) {
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.forEach(this::delete);
  }

  /**
   * Delete unpublished and existing {@link AttachmentState} corresponding to the given path and name.
   *
   * @param path
   * @param name
   */
  public void delete(String path, String name) {
    delete(getAttachmentState(path, name, false));
  }

  /**
   * Make sure there are {@link AttachmentState}s representing the directory and its parents at path.
   *
   * @param path
   */
  public synchronized void mkdirs(String path) {
    if(Strings.isNullOrEmpty(path)) return;

    if(attachmentStateRepository.countByPathAndName(String.format("^%s$", normalizeRegex(path)), DIR_NAME) == 0) {
      // make sure parent exists
      if(path.lastIndexOf('/') > 0) mkdirs(path.substring(0, path.lastIndexOf('/')));
      else if(path.lastIndexOf('/') == 0 && !"/".equals(path)) mkdirs("/");
      Attachment attachment = new Attachment();
      attachment.setId(new ObjectId().toString());
      attachment.setName(DIR_NAME);
      attachment.setPath(path);
      attachment.setLastModifiedDate(DateTime.now());
      attachment.setLastModifiedBy(getCurrentUsername());
      attachmentRepository.save(attachment);
      AttachmentState state = new AttachmentState();
      state.setName(DIR_NAME);
      state.setPath(path);
      state.setAttachment(attachment);
      state.setLastModifiedDate(DateTime.now());
      state.setLastModifiedBy(getCurrentUsername());
      attachmentStateRepository.save(state);
      eventBus.post(new FileUpdatedEvent(state));
    }
  }

  //
  // Publication
  //

  /**
   * Change the publication status of the {@link AttachmentState}.
   *
   * @param state
   * @param publish do the publication or the non publication
   */
  public void publish(AttachmentState state, boolean publish) {
    Map<String, AttachmentState> statesToProcess = Maps.newHashMap();
    publish(state, publish, statesToProcess);
    batchPublish(statesToProcess.values(), getCurrentUsername(), publish);
  }

  /**
   * Change the publication status of the {@link AttachmentState}.
   *
   * @param state
   * @param publish
   * @param statesToProcess
   */
  public void publish(AttachmentState state, boolean publish, Map<String, AttachmentState> statesToProcess) {
    if(publish) {
      // publish the parent directories (if any)
      if(!FileUtils.isRoot(state.getPath())) {
        publishDirs(FileUtils.isDirectory(state) ? FileUtils.getParentPath(state.getPath()) : state.getPath(),
          statesToProcess);
      }
    }
    statesToProcess.put(state.getFullPath(), state);
  }

  /**
   * Change the publication status recursively.
   *
   * @param path
   * @param publish
   */
  public void publish(String path, boolean publish) {
    publish(path, publish, getCurrentUsername());
  }

  /**
   * Change the publication status recursively.
   *
   * @param path
   * @param publish
   * @param publisher
   */
  public void publish(String path, boolean publish, String publisher) {
    fsLock.lock();
    try {
      List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
      states.addAll(findAttachmentStates(String.format("^%s/", path), false));
      Map<String, AttachmentState> statesToProcess = Maps.newHashMap();
      states.forEach(s -> publish(s, publish, statesToProcess));
      batchPublish(statesToProcess.values(), publisher, publish);
    } finally {
      fsLock.unlock();
    }
  }

  private void batchPublish(Collection<AttachmentState> states, String publisher, boolean publish) {
    states.forEach(state -> {
      if (publish) {
        state.publish(publisher);
        state.setRevisionStatus(RevisionStatus.DRAFT);
      } else {
        state.unPublish();
      }

      state.setLastModifiedDate(DateTime.now());
      state.setLastModifiedBy(publisher);
      attachmentStateRepository.save(state);
      eventBus.post(publish ? new FilePublishedEvent(state): new FileUnPublishedEvent(state));
    });
  }

  private void publishWithCascading(String path, boolean publish, String publisher,
      PublishCascadingScope cascadingScope) {
    fsLock.lock();
    try {
      if(PublishCascadingScope.ALL == cascadingScope) {
        publish(path, publish, publisher);
      } else if(PublishCascadingScope.UNDER_REVIEW == cascadingScope) {
        List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
        states.addAll(findAttachmentStates(String.format("^%s/", path), false));
        Map<String, AttachmentState> statesToProcess = Maps.newHashMap();
        states.stream().filter(s -> !publish || s.getRevisionStatus() == RevisionStatus.UNDER_REVIEW)
            .forEach(s -> publish(s, publish, statesToProcess));
        batchPublish(statesToProcess.values(), publisher, publish);
      }
    } finally {
      fsLock.unlock();
    }
  }

  /**
   * Change the publication status of the existing {@link org.obiba.mica.file.AttachmentState}.
   *
   * @param path
   * @param name
   * @param publish
   */
  public void publish(String path, String name, boolean publish) {
    fsLock.lock();
    try {
      publish(getAttachmentState(path, name, false), publish);
    } finally {
      fsLock.unlock();
    }
  }

  //
  // Rename, move and copy
  //

  /**
   * Rename path of all the files found in the given path (and children).
   *
   * @param path
   * @param newPath
   */
  public void rename(String path, String newPath) {
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    // create the directories first (as they could be empty)
    states.stream().filter(FileUtils::isDirectory).forEach(s -> mkdirs(s.getPath().replaceFirst(path, newPath)));
    // then copy the files
    states.stream().filter(s -> !FileUtils.isDirectory(s))
        .forEach(s -> copy(s, s.getPath().replaceFirst(path, newPath), s.getName(), true));
    // mark source as being deleted
    states.stream().filter(FileUtils::isDirectory).forEach(s -> updateStatus(s, RevisionStatus.DELETED));
  }

  /**
   * Rename a specific file at the same path.
   *
   * @param path
   * @param name
   * @param newName
   */
  public void rename(String path, String name, String newName) {
    validateFileName(newName);
    AttachmentState state = getAttachmentState(path, name, false);
    move(state, state.getPath(), newName);
  }

  /**
   * Move a file to another path location.
   *
   * @param path
   * @param name
   * @param newPath
   */
  public void move(String path, String name, String newPath) {
    AttachmentState state = getAttachmentState(path, name, false);
    move(state, newPath, state.getName());
  }

  /**
   * Moving a file consists of copying the file at the provided path and name, and deleting the original one.
   *
   * @param state
   * @param newPath
   * @param newName
   */
  public void move(AttachmentState state, @NotNull String newPath, @NotNull String newName) {
    copy(state, newPath, newName, true);
  }

  /**
   * Copy all files at a given path (and children) into another path location.
   *
   * @param path
   * @param newPath
   */
  public void copy(String path, String newPath) {
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.stream().filter(FileUtils::isDirectory).forEach(s -> mkdirs(s.getPath().replaceFirst(path, newPath)));
    states.stream().filter(s -> !FileUtils.isDirectory(s))
        .forEach(s -> copy(s, s.getPath().replaceFirst(path, newPath), s.getName(), false));
  }

  /**
   * Copy a file into another path location.
   *
   * @param path
   * @param name
   * @param newPath
   */
  public void copy(String path, String name, String newPath) {
    AttachmentState state = getAttachmentState(path, name, false);
    copy(state, newPath, state.getName(), false);
  }

  /**
   * Make a copy of the latest {@link Attachment} (and associated raw file) and optionally delete
   * the {@link AttachmentState} source.
   *
   * @param state
   * @param newPath
   * @param newName
   * @param delete
   */
  public void copy(AttachmentState state, String newPath, String newName, boolean delete) {
    if(state.getPath().equals(newPath) && state.getName().equals(newName)) return;
    if(hasAttachmentState(newPath, newName, false))
      throw new IllegalArgumentException("A file with name '" + newName + "' already exists at path: " + newPath);

    Attachment attachment = state.getAttachment();
    Attachment newAttachment = new Attachment();
    BeanUtils.copyProperties(attachment, newAttachment, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
        "lastModifiedDate");
    newAttachment.setPath(newPath);
    newAttachment.setName(newName);
    save(newAttachment);
    fileStoreService.save(newAttachment.getFileReference(), fileStoreService.getFile(attachment.getFileReference()));
    if(delete) updateStatus(state, RevisionStatus.DELETED);
  }

  /**
   * Reinstate an existing attachment by copying it as a new one, thus generating a new revision
   *
   * @param attachment
   */
  public void reinstate(Attachment attachment) {
    Attachment newAttachment = new Attachment();
    BeanUtils.copyProperties(attachment, newAttachment, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
        "lastModifiedDate");
    newAttachment.setLastModifiedDate(DateTime.now());
    newAttachment.setLastModifiedBy(getCurrentUsername());
    save(newAttachment);
  }

  /**
   * Update {@link RevisionStatus} of all files at a given path (and children).
   *
   * @param path
   * @param status
   */
  public void updateStatus(String path, RevisionStatus status) {
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    AttachmentState state = states.stream().filter(s -> DIR_NAME.equals(s.getName())).findFirst()
        .orElseThrow(() -> NoSuchEntityException.withPath(AttachmentState.class, path));
    RevisionStatus currentStatus = state.getRevisionStatus();
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.forEach(s -> updateStatus(s, status));
    filePublicationFlowNotification.send(path, currentStatus, status);
  }

  /**
   * Update {@link RevisionStatus} of the file with the given path and name.
   *
   * @param path
   * @param name
   * @param status
   */
  public void updateStatus(String path, String name, RevisionStatus status) {
    AttachmentState state = getAttachmentState(path, name, false);
    RevisionStatus currentStatus = state.getRevisionStatus();
    updateStatus(state, status);
    filePublicationFlowNotification.send(path, currentStatus, status);
  }

  /**
   * Update {@link RevisionStatus} of the {@link AttachmentState}.
   *
   * @param state
   * @param status
   */
  public void updateStatus(AttachmentState state, RevisionStatus status) {
    state.setRevisionStatus(status);
    attachmentStateRepository.save(state);
    eventBus.post(new FileUpdatedEvent(state));
  }

  //
  // Query
  //

  public List<AttachmentState> findAttachmentStates(String pathRegEx, boolean publishedFS) {
    return publishedFS ? findPublishedAttachmentStates(pathRegEx) : findDraftAttachmentStates(pathRegEx);
  }

  public List<Attachment> findAttachments(String pathRegEx, boolean publishedFS) {
    return publishedFS ? findDraftAttachments(pathRegEx) : findPublishedAttachments(pathRegEx);
  }

  /**
   * Count the number of {@link AttachmentState}s located at the given path (including the sub-folders). Result excludes the
   * {@link AttachmentState} representing the folder itself.
   *
   * @param path
   * @param publishedFS
   * @return
   */
  public long countAttachmentStates(String path, boolean publishedFS) {
    // count the regular files in the folder
    String pathRegEx = String.format("^%s$", normalizeRegex(path));
    long count = publishedFS
        ? (subjectAclService.isOpenAccess() ? attachmentStateRepository
        .countByPathAndPublishedAttachmentNotNull(pathRegEx) : countAccessiblePublishedAttachmentStates(pathRegEx))
        : attachmentStateRepository.countByPath(pathRegEx);
    count = count == 0 ? 0 : count - 1;

    // count the sub-folders in the folder
    pathRegEx = String.format("^%s/[^/]+$", normalizeRegex(path));
    long dirs = publishedFS
        ? (subjectAclService.isOpenAccess()
        ? attachmentStateRepository.countByPathAndNameAndPublishedAttachmentNotNull(pathRegEx, DIR_NAME)
        : countAccessiblePublishedAttachmentStates(pathRegEx, DIR_NAME))
        : attachmentStateRepository.countByPathAndName(pathRegEx, DIR_NAME);

    return count + dirs;
  }

  /**
   * Get the {@link AttachmentState}, with publication status filter.
   *
   * @param path
   * @param name
   * @param publishedFS published file system view: if true and state is not published, a not found error is thrown
   * @return
   */
  @NotNull
  public AttachmentState getAttachmentState(String path, String name, boolean publishedFS) {
    List<AttachmentState> state = publishedFS
        ? attachmentStateRepository.findByPathAndNameAndPublishedAttachmentNotNull(path, name)
        : attachmentStateRepository.findByPathAndName(path, name);
    if(state.isEmpty()) throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    return state.get(0);
  }

  public boolean hasAttachmentState(String path, String name, boolean publishedFS) {
    String pathRegEx = String.format("^%s$", path);
    return publishedFS
        ? attachmentStateRepository.countByPathAndNameAndPublishedAttachmentNotNull(pathRegEx, name) > 0
        : attachmentStateRepository.countByPathAndName(pathRegEx, name) > 0;
  }

  public List<Attachment> getAttachmentRevisions(AttachmentState state) {
    return attachmentRepository.findByPathAndNameOrderByCreatedDateDesc(state.getPath(), state.getName());
  }

  /**
   * From a path with name, extract the path (can be empty if only one element) and the name (last element).
   *
   * @param pathWithName
   * @param prefix appended to extracted path if not null
   * @return
   */
  public static Pair<String, String> extractPathName(String pathWithName, @Nullable String prefix) {
    return Pair.create(extractDirName(pathWithName, prefix), extractBaseName(pathWithName));
  }

  /**
   * From a path with name, extract the path (can be empty if only one element) and the name (last element).
   *
   * @param pathWithName
   * @return
   */
  public static Pair<String, String> extractPathName(String pathWithName) {
    return extractPathName(pathWithName, null);
  }

  /**
   * Create and return a relative path to the source's parent
   *
   * @param source
   * @param path
   * @return
     */
  public String relativizePaths(String source, String path) {
    return Paths.get(source).getParent().relativize(Paths.get(path)).toString();
  }

  /**
   * Creates a zipped file of the path and it's subdirectories/files
   *
   * @param path
   * @param publishedFS
   * @return
     */
  public String zipDirectory(String path, boolean publishedFS) {
    List<AttachmentState> attachmentStates = getAllowedStates(path, publishedFS);
    String zipName = Paths.get(path).getFileName().toString() + ".zip";

    FileOutputStream fos = null;

    try {
      byte[] buffer = new byte[1024];

      fos = tempFileService.getFileOutputStreamFromFile(zipName);

      ZipOutputStream zos = new ZipOutputStream(fos);

      for (AttachmentState state: attachmentStates) {
        if (FileUtils.isDirectory(state)) {
          zos.putNextEntry(new ZipEntry(relativizePaths(path, state.getFullPath()) + File.separator));
        } else {
          zos.putNextEntry(new ZipEntry(relativizePaths(path, state.getFullPath())));

          InputStream in = fileStoreService.getFile(publishedFS ?
            state.getPublishedAttachment().getFileReference() :
            state.getAttachment().getFileReference());

          int len;
          while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
          }

          in.close();
        }

        zos.closeEntry();
      }

      zos.finish();
    } catch (IOException ioe) {
      Throwables.propagate(ioe);
    } finally {
      IOUtils.closeQuietly(fos);
    }

    return zipName;
  }

  private List<AttachmentState> getAllowedStates(String path, boolean publishedFS) {
    boolean isOpenAccess = micaConfigService.getConfig().isOpenAccess();
    List<AttachmentState> attachmentStates = listDirectoryAttachmentStates(path, publishedFS);
    List<AttachmentState> allowed = attachmentStates.stream()
      .filter(s -> s.getName().equals("."))
      .filter(s -> (isOpenAccess && publishedFS) || subjectAclService.isPermitted(publishedFS ? "/file" : "/draft/file", "VIEW", s.getFullPath()))
      .collect(toList());

    List<AttachmentState> allowedFiles = attachmentStates.stream()
      .filter(s -> allowed.stream().anyMatch(a -> s.getPath().equals(a.getFullPath()) && !s.getName().equals(".")))
      .collect(toList());

    allowed.addAll(allowedFiles);

    return allowed;
  }

  //
  // Event handling
  //

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    log.debug("Study {} was published", event.getPersistable());
    PublishCascadingScope cascadingScope = event.getCascadingScope();
    if(cascadingScope != PublishCascadingScope.NONE) {
      publishWithCascading( //
        String.format("/%s/%s", event.getPersistable().getResourcePath(), event.getPersistable().getId()), //
          true, //
          event.getPublisher(), //
          cascadingScope); //
    }
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.debug("Study {} was unpublished", event.getPersistable());
    publish(
      String.format("/%s/%s", event.getPersistable().getResourcePath(), event.getPersistable().getId()), false
    );
  }

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.debug("Study {} was updated", event.getPersistable());
    fsLock.lock();
    try {
      mkdirs(String.format("/%s/%s", event.getPersistable().getResourcePath(), event.getPersistable().getId()));

      if(event.getPersistable().hasPopulations()) {
        event.getPersistable().getPopulations().stream().filter(Population::hasDataCollectionEvents).forEach(
            p -> p.getDataCollectionEvents().forEach(dce -> mkdirs(String
                .format("/individual-study/%s/population/%s/data-collection-event/%s",
                  event.getPersistable().getId(),
                  p.getId(),
                  dce.getId()))));
      }
    } finally {
      fsLock.unlock();
    }
  }

  @Async
  @Subscribe
  public void networkUpdated(NetworkUpdatedEvent event) {
    log.debug("Network {} was updated", event.getPersistable());
    fsLock.lock();
    try {
      mkdirs(String.format("/network/%s", event.getPersistable().getId()));
    } finally {
      fsLock.unlock();
    }
  }

  @Async
  @Subscribe
  public void networkPublished(NetworkPublishedEvent event) {
    log.debug("Network {} was published", event.getPersistable());
    PublishCascadingScope cascadingScope = event.getCascadingScope();
    if(cascadingScope != PublishCascadingScope.NONE) {
      publishWithCascading( //
          String.format("/network/%s", event.getPersistable().getId()), //
          true, //
          event.getPublisher(), //
          cascadingScope); //
    }
  }

  @Async
  @Subscribe
  public void networkUnpublished(NetworkUnpublishedEvent event) {
    log.debug("Network {} was unpublished", event.getPersistable());
    publish(String.format("/network/%s", event.getPersistable().getId()), false);
  }

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    log.debug("{} {} was updated", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    fsLock.lock();
    try {
      mkdirs(String.format("/%s/%s", getDatasetTypeFolder(event.getPersistable()), event.getPersistable().getId()));
    } finally {
      fsLock.unlock();
    }
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    PublishCascadingScope cascadingScope = event.getCascadingScope();
    if(cascadingScope != PublishCascadingScope.NONE) {
      publishWithCascading( //
          String.format("/%s/%s", getDatasetTypeFolder(event.getPersistable()), event.getPersistable().getId()), //
          true, //
          event.getPublisher(), //
          cascadingScope); //
    }
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    publish(String.format("/%s/%s", getDatasetTypeFolder(event.getPersistable()), event.getPersistable().getId()),
        false);
  }

  @Async
  @Subscribe
  public void projectUpdated(ProjectUpdatedEvent event) {
    log.debug("Project {} was updated", event.getPersistable());
    fsLock.lock();
    try {
      mkdirs(String.format("/project/%s", event.getPersistable().getId()));
    } finally {
      fsLock.unlock();
    }
  }

  @Async
  @Subscribe
  public void projectPublished(ProjectPublishedEvent event) {
    log.debug("Project {} was published", event.getPersistable());
    PublishCascadingScope cascadingScope = event.getCascadingScope();
    if(cascadingScope != PublishCascadingScope.NONE) {
      publishWithCascading( //
        String.format("/project/%s", event.getPersistable().getId()), //
        true, //
        event.getPublisher(), //
        cascadingScope); //
    }
  }

  @Async
  @Subscribe
  public void projectUnpublished(ProjectUnpublishedEvent event) {
    log.debug("Project {} was unpublished", event.getPersistable());
    publish(String.format("/project/%s", event.getPersistable().getId()), false);
  }

  private String getDatasetTypeFolder(Dataset dataset) {
    String type = "collected-dataset";
    if(dataset instanceof HarmonizationDataset) {
      type = "harmonized-dataset";
    }
    return type;
  }

  //
  // Private methods
  //

  /**
   * When publishing a file, in order to be able to browse to this file through the parent folders, publish all the
   * parent folders.
   *
   * @param path
   */
  private synchronized void publishDirs(String path, Map<String, AttachmentState> statesToProcess) {
    if(Strings.isNullOrEmpty(path)) return;
    List<AttachmentState> states = attachmentStateRepository.findByPathAndName(path, DIR_NAME);
    if(states.isEmpty()) return;

    if(path.lastIndexOf('/') > 0) publishDirs(path.substring(0, path.lastIndexOf('/')), statesToProcess);
    else if(path.lastIndexOf('/') == 0 && !"/".equals(path)) publishDirs("/", statesToProcess);

    AttachmentState state = states.get(0);
    if(state.isPublished()) return;
    statesToProcess.put(state.getFullPath(), state);
  }

  /**
   * Find all the draft attachment states matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<AttachmentState> findDraftAttachmentStates(String pathRegEx) {
    return attachmentStateRepository.findByPath(normalizeRegex(pathRegEx)).stream().collect(toList());
  }

  /**
   * Find all the published attachment states matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<AttachmentState> findPublishedAttachmentStates(String pathRegEx) {
    return attachmentStateRepository.findByPathAndPublishedAttachmentNotNull(normalizeRegex(pathRegEx)).stream()
        .collect(toList());
  }

  /**
   * Find all the draft attachments matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<Attachment> findDraftAttachments(String pathRegEx) {
    return findDraftAttachmentStates(pathRegEx).stream().map(AttachmentState::getAttachment).collect(toList());
  }

  /**
   * Find all the published attachments matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<Attachment> findPublishedAttachments(String pathRegEx) {
    return findPublishedAttachmentStates(pathRegEx).stream().map(AttachmentState::getPublishedAttachment)
        .collect(toList());
  }

  /**
   * Get the count of accessible files at path.
   *
   * @param pathRegEx
   * @return
   */
  private long countAccessiblePublishedAttachmentStates(String pathRegEx) {
    return attachmentStateRepository.findByPathAndPublishedAttachmentNotNull(pathRegEx).stream()
        .filter(s -> subjectAclService.isAccessible("/file", s.getFullPath())) //
        .count();
  }

  /**
   * Get the count of named accessible files at path.
   *
   * @param pathRegEx
   * @param name
   * @return
   */
  private long countAccessiblePublishedAttachmentStates(String pathRegEx, String name) {
    return attachmentStateRepository.findByPathAndPublishedAttachmentNotNull(pathRegEx).stream()
        .filter(s -> s.getName().equals(name) && subjectAclService.isAccessible("/file", s.getFullPath())) //
        .count();
  }

  private static String extractDirName(String pathWithName, @Nullable String prefix) {
    String dir = pathWithName.contains("/") ? pathWithName.substring(0, pathWithName.lastIndexOf('/')) : "";
    return Strings.isNullOrEmpty(prefix)
        ? dir
        : Strings.isNullOrEmpty(dir) ? prefix : String.format("%s/%s", prefix, dir);
  }

  private static String extractBaseName(String pathWithName) {
    return pathWithName.contains("/") ? pathWithName.substring(pathWithName.lastIndexOf('/') + 1) : pathWithName;
  }

  private String getCurrentUsername() {
    Subject subject = SecurityUtils.getSubject();

    try {
      if(subject != null && subject.getPrincipal() != null)
        return subject.getPrincipal().toString();
    } catch (UnknownSessionException ignore) {
      log.debug(String.format(
        "Impossible to get currentUsername, we are probably in an @Async method. Use DEFAULT_AUTHOR_NAME [%s]", AbstractGitWriteCommand.DEFAULT_AUTHOR_NAME), ignore);
    }

    return AbstractGitWriteCommand.DEFAULT_AUTHOR_NAME;
  }

  private void validateFileName(String name) {
    Pattern pattern = Pattern.compile("[\\$%/#]");
    Matcher matcher = pattern.matcher(name);
    if(matcher.find()) {
      throw new InvalidFileNameException(name);
    }
  }

  private String normalizeRegex(String path) {
    return FileUtils.normalizeRegex(path);
  }

  /**
   * Creates a list of {@link AttachmentState}s in and under the path's directory tree
   *
   * @param path
   * @param publishedFS
   * @return
   */
  private List<AttachmentState> listDirectoryAttachmentStates(String path, boolean publishedFS) {
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), publishedFS);
    states.addAll(findAttachmentStates(String.format("^%s/", path), publishedFS));

    if (publishedFS && !subjectAclService.isOpenAccess()) {
      return states.stream().filter(s -> subjectAclService.isAccessible("/file", s.getFullPath())).collect(toList());
    }

    return states;
  }
}
