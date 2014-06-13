package org.obiba.mica.file;

import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Persistable;

public interface PersistableWithAttachments extends Persistable<String> {

  Iterable<Attachment> getAllAttachments();

  @NotNull
  Attachment findAttachmentById(String attachmentId);

}
