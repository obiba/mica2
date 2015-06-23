package org.obiba.mica.core.repository;

import org.obiba.mica.core.domain.AttachmentAware;

public interface AttachmentAwareRepository<T extends AttachmentAware> {
  public T saveWithAttachments(T obj, boolean removeOrphanedAttachments);

  public void deleteWithAttachments(T obj, boolean removeOrphanedAttachments);
}
