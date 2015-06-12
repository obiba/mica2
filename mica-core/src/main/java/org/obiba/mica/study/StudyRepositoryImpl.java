package org.obiba.mica.study;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AbstractAttachmentAwareRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.study.domain.Study;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class StudyRepositoryImpl extends AbstractAttachmentAwareRepository<Study> implements StudyRepositoryCustom {

  @Inject
  AttachmentRepository attachmentRepository;

  @Override
  public Study saveWithAttachments(Study obj) {
    obj.getPopulations().forEach(p -> {
        try {
          p.getDataCollectionEvents().forEach(d -> attachmentRepository.save(d.getAttachments()));
        } catch(DuplicateKeyException ex) {
          //ignore
        }
      }
    );

    Study res = super.saveWithAttachments(obj);

    obj.getPopulations().forEach(p ->
        p.getDataCollectionEvents().forEach(d -> attachmentRepository.delete(d.removedAttachments()))
    );

    return res;
  }

  @Override
  public void deleteWithAttachments(Study obj) {
    obj.getPopulations().forEach(p ->
        p.getDataCollectionEvents().forEach(d -> attachmentRepository.delete(d.getAttachments()))
    );

    super.deleteWithAttachments(obj);
  }
}
