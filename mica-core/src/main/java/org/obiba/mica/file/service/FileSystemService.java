package org.obiba.mica.file.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.util.Pair;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.obiba.git.command.AbstractGitWriteCommand;
import org.obiba.mica.NoSuchEntityException;
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
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.network.event.NetworkUpdatedEvent;
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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

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
      if(saved == null) {
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
    if(state.isNew()) {
      if(FileUtils.isDirectory(state)) {
        mkdirs(getParentPath(saved.getPath()));
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
    states.stream().forEach(this::delete);
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

    if(attachmentStateRepository.countByPathAndName(path, DIR_NAME) == 0) {
      // make sure parent exists
      if(path.lastIndexOf('/') > 0) mkdirs(path.substring(0, path.lastIndexOf('/')));
      else if(path.lastIndexOf('/') == 0 && !"/".equals(path)) mkdirs("/");
      Attachment attachment = new Attachment();
      attachment.setId(new ObjectId().toString());
      attachment.setName(DIR_NAME);
      attachment.setPath(path);
      attachment.setLastModifiedDate(DateTime.now());
      attachmentRepository.save(attachment);
      AttachmentState state = new AttachmentState();
      state.setName(DIR_NAME);
      state.setPath(path);
      state.setAttachment(attachment);
      state.setLastModifiedDate(DateTime.now());
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
    publish(state, publish, getCurrentUsername());
  }

  /**
   * Change the publication status of the {@link AttachmentState}.
   *
   * @param state
   * @param publish
   * @param publisher
   */
  public void publish(AttachmentState state, boolean publish, String publisher) {
    if(publish) {
      // publish the parent directories (if any)
      if(!FileUtils.isRoot(state.getPath())) {
        publishDirs(getParentPath(state.getPath()), publisher);
      }
      state.publish(publisher);
      state.setRevisionStatus(RevisionStatus.DRAFT);
    } else state.unPublish();
    state.setLastModifiedDate(DateTime.now());
    attachmentStateRepository.save(state);
    if(publish) eventBus.post(new FilePublishedEvent(state));
    else eventBus.post(new FileUnPublishedEvent(state));
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
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.stream().forEach(s -> publish(s, publish, publisher));
  }

  /**
   * Change the publication status of the existing {@link org.obiba.mica.file.AttachmentState}.
   *
   * @param path
   * @param name
   * @param publish
   */
  public void publish(String path, String name, boolean publish) {
    publish(getAttachmentState(path, name, false), publish);
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
    states.stream().forEach(s -> copy(s, newPath, s.getName(), false));
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
   * Make a copy of the latest {@link org.obiba.mica.file.Attachment} (and associated raw file) and optionally delete
   * the {@link org.obiba.mica.file.AttachmentState} source.
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
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.stream().forEach(s -> updateStatus(s, status));
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
    updateStatus(state, status);
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
   * Count the number of {@link AttachmentState}s located at the given path. Result excludes the
   * {@link AttachmentState} representing the folder itself.
   *
   * @param path
   * @param publishedFS
   * @return
   */
  public long countAttachmentStates(String path, boolean publishedFS) {
    long count = publishedFS
      ? attachmentStateRepository.countByPathAndPublishedAttachmentNotNull(path)
      : attachmentStateRepository.countByPath(path);
    return count == 0 ? 0 : count - 1;
  }

  /**
   * Get the {@link org.obiba.mica.file.AttachmentState}, with publication status filter.
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
    return publishedFS
      ? attachmentStateRepository.countByPathAndNameAndPublishedAttachmentNotNull(path, name) > 0
      : attachmentStateRepository.countByPathAndName(path, name) > 0;
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

  //
  // Event handling
  //

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    log.debug("Study {} was published", event.getPersistable());
    publish(String.format("/study/%s", event.getPersistable().getId()), true, event.getPublisher());
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.debug("Study {} was unpublished", event.getPersistable());
    publish(String.format("/study/%s", event.getPersistable().getId()), false);
  }

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.debug("Study {} was updated", event.getPersistable());
    if(event.getPersistable().hasPopulations()) {
      event.getPersistable().getPopulations().stream().filter(Population::hasDataCollectionEvents).forEach(p -> {
        p.getDataCollectionEvents().forEach(dce -> mkdirs(String
          .format("/study/%s/population/%s/data-collection-event/%s", event.getPersistable().getId(), p.getId(),
            dce.getId())));
      });
    } else mkdirs(String.format("/study/%s", event.getPersistable().getId()));
  }

  @Async
  @Subscribe
  public void networkUpdated(NetworkUpdatedEvent event) {
    log.debug("Network {} was updated", event.getPersistable());
    mkdirs(String.format("/network/%s", event.getPersistable().getId()));
  }

  @Async
  @Subscribe
  public void networkPublished(NetworkPublishedEvent event) {
    log.debug("Network {} was published", event.getPersistable());
    publish(String.format("/network/%s", event.getPersistable().getId()), true, event.getPublisher());
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
    mkdirs(String.format("/%s/%s", getDatasetTypeFolder(event.getPersistable()), event.getPersistable().getId()));
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    publish(String.format("/%s/%s", getDatasetTypeFolder(event.getPersistable()), event.getPersistable().getId()), true,
      event.getPublisher());
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    publish(String.format("/%s/%s", getDatasetTypeFolder(event.getPersistable()), event.getPersistable().getId()),
      false);
  }

  private String getDatasetTypeFolder(Dataset dataset) {
    String type = "study-dataset";
    if(dataset instanceof HarmonizationDataset) {
      type = "harmonization-dataset";
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
  private synchronized void publishDirs(String path, String publisher) {
    if(Strings.isNullOrEmpty(path)) return;
    List<AttachmentState> states = attachmentStateRepository.findByPathAndName(path, DIR_NAME);
    if(states.isEmpty()) return;

    if(path.lastIndexOf('/') > 0) publishDirs(path.substring(0, path.lastIndexOf('/')), publisher);
    else if(path.lastIndexOf('/') == 0 && !"/".equals(path)) publishDirs("/", publisher);

    AttachmentState state = states.get(0);
    if(state.isPublished()) return;
    state.publish(publisher);
    state.setLastModifiedDate(DateTime.now());
    attachmentStateRepository.save(state);
    eventBus.post(new FilePublishedEvent(state));
  }

  /**
   * Find all the draft attachment states matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<AttachmentState> findDraftAttachmentStates(String pathRegEx) {
    return attachmentStateRepository.findByPath(pathRegEx).stream().collect(Collectors.toList());
  }

  /**
   * Find all the published attachment states matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<AttachmentState> findPublishedAttachmentStates(String pathRegEx) {
    return attachmentStateRepository.findByPathAndPublishedAttachmentNotNull(pathRegEx).stream()
      .collect(Collectors.toList());
  }

  /**
   * Find all the draft attachments matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<Attachment> findDraftAttachments(String pathRegEx) {
    return findDraftAttachmentStates(pathRegEx).stream().map(AttachmentState::getAttachment)
      .collect(Collectors.toList());
  }

  /**
   * Find all the published attachments matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  private List<Attachment> findPublishedAttachments(String pathRegEx) {
    return findPublishedAttachmentStates(pathRegEx).stream().map(AttachmentState::getPublishedAttachment)
      .collect(Collectors.toList());
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
    return subject == null || subject.getPrincipal() == null
      ? AbstractGitWriteCommand.DEFAULT_AUTHOR_NAME
      : subject.getPrincipal().toString();
  }

  private String getParentPath(String path) {
    int idx = path.lastIndexOf('/');
    return idx == 0 ? "/" : path.substring(0, idx);
  }

  private void validateFileName(String name) {
    Pattern pattern = Pattern.compile("[\\$%/#]");
    Matcher matcher = pattern.matcher(name);
    if (matcher.find()) {
      throw new InvalidFileNameException(name);
    }
  }
}
