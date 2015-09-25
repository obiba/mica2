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
  public Study saveWithAttachments(Study study, boolean removeOrphanedAttachments) {
    study.getPopulations().forEach(p -> p.getDataCollectionEvents().forEach(d -> d.getAttachments().forEach(a -> {
      try {
        a.setPath(
          String.format("/study/%s/population/%s/data-collection-event/%s/attachment", study.getId(), p.getId(), d.getId()));
        attachmentRepository.save(a);
      } catch(DuplicateKeyException | OptimisticLockingFailureException ex) {
        //TODO: copy same attachments that are in different DCEs.
      }
    })));

    Study res = super.saveWithAttachments(study, removeOrphanedAttachments);

    return res;
  }

  @Override
  public void deleteWithAttachments(Study study, boolean removeOrphanedAttachments) {
    study.getPopulations()
      .forEach(p -> p.getDataCollectionEvents().forEach(d -> attachmentRepository.delete(d.getAttachments())));

    super.deleteWithAttachments(study, removeOrphanedAttachments);
  }

  @Override
  protected String getAttachmentPath(Study study) {
    return String.format("/study/%s/attachment", study.getId());
  }
}
