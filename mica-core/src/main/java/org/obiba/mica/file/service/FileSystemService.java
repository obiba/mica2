package org.obiba.mica.file.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.math3.util.Pair;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.file.event.FilePublishedEvent;
import org.obiba.mica.file.event.FileUnPublishedEvent;
import org.obiba.mica.file.event.FileUpdatedEvent;
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

    if (attachment.isNew()) {
      attachment.setId(new ObjectId().toString());
    } else {
      saved = attachmentRepository.findOne(attachment.getId());
      if(saved == null) {
        saved = attachment;
      }
      else {
        BeanUtils.copyProperties(attachment, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
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

    List<AttachmentState> states = attachmentStateRepository
      .findByPathAndName(saved.getPath(), saved.getName());
    AttachmentState state = states.isEmpty() ? new AttachmentState() : states.get(0);
    state.setAttachment(saved);
    state.setLastModifiedDate(DateTime.now());
    attachmentStateRepository.save(state);

    eventBus.post(new FileUpdatedEvent(state));
  }

  /**
   * Make sure the {@link org.obiba.mica.file.AttachmentState} is not published and delete it.
   *
   * @param state
   */
  public void delete(AttachmentState state) {
    if(state.isPublished()) publish(state, false);
    attachmentStateRepository.delete(state);
    eventBus.post(new FileDeletedEvent(state));
  }

  /**
   * Delete all unpublished {@link org.obiba.mica.file.AttachmentState}s corresponding to the given path.
   *
   * @param path
   */
  public void delete(String path) {
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.stream().filter(s -> !s.isPublished()).forEach(this::delete);
  }

  /**
   * Delete unpublished and existing {@link org.obiba.mica.file.AttachmentState} corresponding to the given path and name.
   *
   * @param path
   * @param name
   */
  public void delete(String path, String name) {
    delete(getAttachmentState(path, name, false));
  }

  //
  // Publication
  //

  /**
   * Change the publication status of the {@link org.obiba.mica.file.AttachmentState}.
   *
   * @param state
   * @param publish do the publication or the non publication
   */
  public void publish(AttachmentState state, boolean publish) {
    if(state.isPublished() && publish) return;
    if(!state.isPublished() && !publish) return;
    if(publish) state.publish();
    else state.unPublish();
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
    List<AttachmentState> states = findAttachmentStates(String.format("^%s$", path), false);
    states.addAll(findAttachmentStates(String.format("^%s/", path), false));
    states.stream().forEach(s -> publish(s, publish));
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
    states.stream().forEach(s -> copy(s, s.getPath().replaceFirst(path, newPath), s.getName(), true));
  }

  /**
   * Rename a specific file at the same path.
   *
   * @param path
   * @param name
   * @param newName
   */
  public void rename(String path, String name, String newName) {
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
    if(hasAttachmentState(newPath, newName, false))
      throw new IllegalArgumentException("A file with name '" + newName + "' already exists at path: " + newPath);

    Attachment attachment = state.getAttachment();
    Attachment newAttachment = new Attachment();
    BeanUtils.copyProperties(attachment, newAttachment, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
      "lastModifiedDate");
    newAttachment.setPath(newPath);
    newAttachment.setName(newName);
    save(newAttachment);
    fileStoreService.save(newAttachment.getId(), fileStoreService.getFile(attachment.getId()));
    if(delete) updateStatus(state, RevisionStatus.DELETED);
  }

  /**
   * Reinstate an existing attachment by copying it as a new one, thus generating a new revision
   * @param attachment
   */
  public void reinstate(Attachment attachment) {
    Attachment newAttachment = new Attachment();
    BeanUtils.copyProperties(attachment, newAttachment, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
      "lastModifiedDate");
    save(newAttachment);
//    fileService.save(newAttachment.getId(), fileService.getFile(attachment.getId()));
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
   * Get the {@link org.obiba.mica.file.AttachmentState}, with publication status filter.
   *
   * @param path
   * @param name
   * @param publishedFS published file system view: if true and state is not published, a not found error is thrown
   * @return
   */
  @NotNull
  public AttachmentState getAttachmentState(String path, String name, boolean publishedFS) {
    List<AttachmentState> state = attachmentStateRepository.findByPathAndName(path, name);
    if(state.isEmpty()) throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    if(publishedFS && !state.get(0).isPublished())
      throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    return state.get(0);
  }

  public boolean hasAttachmentState(String path, String name, boolean publishedFS) {
    List<AttachmentState> state = attachmentStateRepository.findByPathAndName(path, name);
    return !state.isEmpty();
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
    log.info("Study {} was published", event.getPersistable());
    publish(String.format("/study/%s", event.getPersistable().getId()), true);
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.info("Study {} was unpublished", event.getPersistable());
    publish(String.format("/study/%s", event.getPersistable().getId()), false);
  }

  //
  // Private methods
  //

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
    return attachmentStateRepository.findByPath(pathRegEx).stream().filter(AttachmentState::isPublished)
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
}
