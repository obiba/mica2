package org.obiba.mica.file;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.GitPersistable;

public interface PersistableWithAttachments extends GitPersistable {

  Iterable<Attachment> getAllAttachments();

  @NotNull
  Attachment findAttachmentById(String attachmentId);

}
