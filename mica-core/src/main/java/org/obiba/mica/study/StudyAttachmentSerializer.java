package org.obiba.mica.study;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.inject.Inject;

import org.obiba.git.command.AddDeleteFilesCommand;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentSerializer;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.TempFileService;
import org.obiba.mica.service.GitService;
import org.obiba.mica.study.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

@Component
public class StudyAttachmentSerializer implements AttachmentSerializer<Study> {

  private static final Logger log = LoggerFactory.getLogger(StudyAttachmentSerializer.class);

  @Inject
  private TempFileService tempFileService;

  @Override
  public void serializeAttachments(Study study, Collection<String> existingPathsInRepo,
      AddDeleteFilesCommand.Builder builder) {

    log.debug("existingPathsInRepo: {}", existingPathsInRepo);

    Collection<String> filesToDelete = new HashSet<>(existingPathsInRepo);

    if(study.getAttachments() != null) {
      for(Attachment attachment : study.getAttachments()) {
        processAttachment(attachment, study, builder, filesToDelete);
      }
    }
    if(study.getPopulations() != null) {
      study.getPopulations().stream() //
          .filter(population -> population.getDataCollectionEvents() != null) //
          .forEach(population -> population.getDataCollectionEvents().stream() //
              .filter(dce -> dce.getAttachments() != null) //
              .forEach(dce -> dce.getAttachments()
                  .forEach(attachment -> processAttachment(attachment, dce, builder, filesToDelete))));
    }

    log.debug("filesToDelete: {}", filesToDelete);

    filesToDelete.forEach(builder::deleteFile);
  }

  private void processAttachment(Attachment attachment, Persistable<String> parent,
      AddDeleteFilesCommand.Builder builder, Collection<String> filesToDelete) {
    String pathInRepo = GitService.getPathInRepo(attachment.getId());
    if(attachment.isJustUploaded()) {
      TempFile tempFile = tempFileService.getMetadata(attachment.getId());
      builder.addFile(pathInRepo, new ByteArrayInputStream(tempFileService.getContent(attachment.getId())));
      attachment.setName(tempFile.getName());
      attachment.setSize(tempFile.getSize());
      attachment.setMd5(tempFile.getMd5());
      attachment.setJustUploaded(false);
      tempFileService.delete(attachment.getId());
    }
    filesToDelete.remove(pathInRepo);

  }

}
