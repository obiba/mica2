package org.obiba.mica.core.repository;

import javax.inject.Inject;

import org.obiba.mica.core.domain.AttachmentAware;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

public class AbstractAttachmentAwareRepository<T extends AttachmentAware> {
  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  MongoTemplate mongoTemplate;

  public T saveWithAttachments(T obj) {
    obj.getAttachments().forEach(a -> {
      try{
        attachmentRepository.save(a);
      } catch(DuplicateKeyException ex) {
        //ignore
      }
    });

    mongoTemplate.save(obj);
    attachmentRepository.delete(obj.removedAttachments());

    return obj;
  }

  public void deleteWithAttachments(T obj) {
    mongoTemplate.remove(obj);
    attachmentRepository.delete(obj.getAttachments());
  }
}
