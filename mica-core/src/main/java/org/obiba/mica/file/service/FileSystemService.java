package org.obiba.mica.file.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

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
import com.google.common.collect.Lists;
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
      .findByNameAndPath(attachment.getName(), attachment.getPath());
    AttachmentState state = states.isEmpty() ? new AttachmentState() : states.get(0);
    state.setAttachment(attachment);
    attachmentStateRepository.save(state);
  }

  /**
   * Find all the attachments in draft state matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  public List<Attachment> findDraftAttachments(String pathRegEx) {
    List<Attachment> attachments = Lists.newArrayList();
    attachmentStateRepository.findByPath(pathRegEx).stream().map(AttachmentState::getAttachment)
      .forEach(attachments::add);
    return attachments;
  }

  /**
   * Find all the attachments in published state matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  public List<Attachment> findPublishedAttachments(String pathRegEx) {
    List<Attachment> attachments = Lists.newArrayList();
    attachmentStateRepository.findByPath(pathRegEx).stream().filter(AttachmentState::isPublished)
      .map(AttachmentState::getPublishedAttachment).forEach(attachments::add);
    return attachments;
  }

  @NotNull
  public Attachment getDraftAttachment(String path, String name) {
    List<AttachmentState> state = attachmentStateRepository.findByNameAndPath(name, path);
    if(state.isEmpty())
      throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    return state.get(0).getAttachment();
  }

  @NotNull
  public Attachment getPublishedAttachment(String path, String name) {
    List<AttachmentState> state = attachmentStateRepository.findByNameAndPath(name, path);
    if(state.isEmpty() || !state.get(0).isPublished())
      throw NoSuchEntityException.withPath(Attachment.class, path + "/" + name);
    return state.get(0).getPublishedAttachment();
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
    attachmentStateRepository.findByPath(pathRegEx).forEach(state -> {
      state.publish();
      attachmentStateRepository.save(state);
    });
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.info("Study {} was unpublished", event.getPersistable());
    String pathRegEx = String.format("^/study/%s", event.getPersistable().getId());
    attachmentStateRepository.findByPath(pathRegEx).forEach(state -> {
      state.unPublish();
      attachmentStateRepository.save(state);
    });
  }

  //
  // Private methods
  //

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
