package org.obiba.mica.study;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.repository.AttachmentAwareRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.ContactAwareRepository;
import org.obiba.mica.core.repository.ContactRepository;
import org.obiba.mica.file.FileService;
import org.obiba.mica.study.domain.Study;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudyRepositoryImpl
  implements StudyRepositoryCustom, AttachmentAwareRepository<Study>, ContactAwareRepository<Study> {

  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  ContactRepository contactRepository;

  @Inject
  FileService fileService;

  @Inject
  MongoTemplate mongoTemplate;

  @Override
  public AttachmentRepository getAttachmentRepository() {
    return attachmentRepository;
  }

  @Override
  public FileService getFileService() {
    return fileService;
  }

  @Override
  public ContactRepository getContactRepository() {
    return contactRepository;
  }

  @Override
  public List<Contact> findAllContactsByParent(Study study) {
    return contactRepository.findByStudyIds(study.getId());
  }

  @Override
  public Study saveWithReferences(Study study) {
    study.getPopulations().forEach(p -> p.getDataCollectionEvents().forEach(d -> d.getAttachments().forEach(a -> {
      try {
        String defaultPath = String.format("/study/%s/attachments/population/%s/data-collection-event/%s", study.getId(), p.getId(), d.getId());
        if(!a.hasPath() || !a.getPath().startsWith(defaultPath)) a.setPath(defaultPath);
        attachmentRepository.save(a);
      } catch(DuplicateKeyException | OptimisticLockingFailureException ex) {
        //TODO: copy same attachments that are in different DCEs.
      }
    })));

    saveAttachments(study);
    saveContacts(study);
    mongoTemplate.save(study);
    updateRemovedContacts(study);

    return study;
  }

  @Override
  public void deleteWithReferences(Study study) {
    mongoTemplate.remove(study);
    deleteAttachments(study);
    deleteContacts(study);
  }

  @Override
  public String getAttachmentPath(Study study) {
    return String.format("/study/%s/attachments", study.getId());
  }
}
