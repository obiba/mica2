package org.obiba.mica.core.repository;

import javax.inject.Inject;

import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.file.Attachment;
import org.springframework.data.mongodb.core.MongoTemplate;

public abstract class AbstractAttachmentAwareRepository<T extends AttachmentAware> implements AttachmentAwareRepository<T> {
  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  MongoTemplate mongoTemplate;

  @Override
  public T saveWithAttachments(T obj, boolean removeOrphanedAttachments) {
    obj.getAttachments().forEach(a -> {
      a.setPath(getAttachmentPath(obj, a));
      attachmentRepository.save(a);
    });

    mongoTemplate.save(obj);

    if(removeOrphanedAttachments) attachmentRepository.delete(obj.removedAttachments());

    return obj;
  }

  @Override
  public void deleteWithAttachments(T obj, boolean removeOrphanedAttachments) {
    mongoTemplate.remove(obj);

    if(removeOrphanedAttachments) attachmentRepository.delete(obj.getAttachments());
  }

  protected abstract String getAttachmentPath(T obj, Attachment attachment);
}
