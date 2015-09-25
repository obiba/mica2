package org.obiba.mica.core.repository;

import javax.inject.Inject;

import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.file.GridFsService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

public abstract class AbstractAttachmentAwareRepository<T extends AttachmentAware> implements AttachmentAwareRepository<T> {
  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  GridFsService gridFsService;

  @Inject
  MongoTemplate mongoTemplate;

  @Override
  public T saveWithAttachments(T obj, boolean removeOrphanedAttachments) {
    obj.getAttachments().forEach(a -> {
      try {
        a.setPath(getAttachmentPath(obj));
        attachmentRepository.save(a);
      } catch(DuplicateKeyException ex) {
        //ignore
      }
    });

    mongoTemplate.save(obj);

    if(removeOrphanedAttachments) {
      obj.removedAttachments().forEach(a -> {
        attachmentRepository.delete(a);
        gridFsService.delete(a.getId());
      });
    }

    return obj;
  }

  @Override
  public void deleteWithAttachments(T obj, boolean removeOrphanedAttachments) {
    mongoTemplate.remove(obj);

    if(removeOrphanedAttachments) {
      attachmentRepository.findByPath(String.format("^%s", getAttachmentPath(obj).replaceAll("/attachment$", "")))
        .forEach(a -> {
          attachmentRepository.delete(a);
          gridFsService.delete(a.getId());
        });
    }
  }

  protected abstract String getAttachmentPath(T obj);
}
