package org.obiba.mica.study;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AbstractAttachmentAwareRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.study.domain.Study;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class StudyRepositoryImpl extends AbstractAttachmentAwareRepository<Study> implements StudyRepositoryCustom {

  @Inject
  AttachmentRepository attachmentRepository;

  @Override
  public Study saveWithAttachments(Study obj, boolean removeOrphanedAttachments) {
    obj.getPopulations().forEach(p -> p.getDataCollectionEvents().forEach(d -> d.getAttachments().forEach(a -> {
        try {
          attachmentRepository.save(a);
        } catch(DuplicateKeyException | OptimisticLockingFailureException ex) {
          //TODO: copy same attachments that are in different DCEs.
        }
      })));

    Study res = super.saveWithAttachments(obj, removeOrphanedAttachments);

    return res;
  }

  @Override
  public void deleteWithAttachments(Study obj, boolean removeOrphanedAttachments) {
    obj.getPopulations()
      .forEach(p -> p.getDataCollectionEvents().forEach(d -> attachmentRepository.delete(d.getAttachments())));

    super.deleteWithAttachments(obj, removeOrphanedAttachments);
  }
}
