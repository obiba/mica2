package org.obiba.mica.file.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileService;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;

import javafx.util.Pair;

@Component
public class FileSystemService {

  private static final Logger log = LoggerFactory.getLogger(FileSystemService.class);

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private AttachmentStateRepository attachmentStateRepository;

  @Inject
  private FileService fileService;

  public void save(Attachment attachment) {
    if(attachment.isJustUploaded()) {
      if(attachmentRepository.exists(attachment.getId())) {
        // replace already existing attachment
        fileService.delete(attachment.getId());
        attachmentRepository.delete(attachment.getId());
      }
      fileService.save(attachment.getId());
      attachment.setJustUploaded(false);
    }
    attachmentRepository.save(attachment);

    List<AttachmentState> states = attachmentStateRepository
      .findByPathAndName(attachment.getPath(), attachment.getName());
    AttachmentState state = states.isEmpty() ? new AttachmentState() : states.get(0);
    state.setAttachment(attachment);
    state.setLastModifiedDate(DateTime.now());
    attachmentStateRepository.save(state);
  }

  public List<AttachmentState> findAttachmentStates(String pathRegEx, boolean published) {
    return published ? findPublishedAttachmentStates(pathRegEx) : findDraftAttachmentStates(pathRegEx);
  }

  public List<Attachment> findAttachments(String pathRegEx, boolean published) {
    return published ? findDraftAttachments(pathRegEx) : findPublishedAttachments(pathRegEx);
  }

  /**
   * Get the atachment state, with publication status filter.
   *
   * @param path
   * @param name
   * @param published if true and state is not published, an not found error is thrown
   * @return
   */
  @NotNull
  public AttachmentState getAttachmentState(String path, String name, boolean published) {
    List<AttachmentState> state = attachmentStateRepository.findByPathAndName(path, name);
    if(state.isEmpty()) throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    if(published && !state.get(0).isPublished())
      throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    return state.get(0);
  }

  /**
   * From a path with name, extract the path (can be empty if only one element) and the name (last element).
   *
   * @param pathWithName
   * @param prefix appended to extracted path if not null
   * @return
   */
  public static Pair<String, String> extractPathName(String pathWithName, @Nullable String prefix) {
    return new Pair<>(extractDirName(pathWithName, prefix), extractBaseName(pathWithName));
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
    String pathRegEx = String.format("^/study/%s", event.getPersistable().getId());
    List<AttachmentState> states = attachmentStateRepository.findByPath(pathRegEx + "$");
    states.addAll(attachmentStateRepository.findByPath(pathRegEx + "/"));
    states.forEach(state -> {
      state.publish();
      attachmentStateRepository.save(state);
    });
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.info("Study {} was unpublished", event.getPersistable());
    String pathRegEx = String.format("^/study/%s", event.getPersistable().getId());
    List<AttachmentState> states = attachmentStateRepository.findByPath(pathRegEx + "$");
    states.addAll(attachmentStateRepository.findByPath(pathRegEx + "/"));
    states.forEach(state -> {
      state.unPublish();
      attachmentStateRepository.save(state);
    });
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
