package org.obiba.mica.file.service;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
      if (attachmentRepository.exists(attachment.getId())) {
        // replace already existing attachment
        fileService.delete(attachment.getId());
        attachmentRepository.delete(attachment.getId());
      }
      fileService.save(attachment.getId());
      attachment.setJustUploaded(false);
    }
    attachmentRepository.save(attachment);

    List<AttachmentState> states = attachmentStateRepository.findByNameAndPath(attachment.getName(),
      attachment.getPath());
    AttachmentState state = states.isEmpty() ? new AttachmentState() : states.get(0);
    state.setAttachment(attachment);
    attachmentStateRepository.save(state);
  }
}
