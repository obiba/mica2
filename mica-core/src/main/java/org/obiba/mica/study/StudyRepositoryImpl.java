package org.obiba.mica.study;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.repository.ContactAwareRepository;
import org.obiba.mica.core.repository.ContactRepository;
import org.obiba.mica.file.FileService;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudyRepositoryImpl implements StudyRepositoryCustom, ContactAwareRepository<Study> {

  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  AttachmentStateRepository attachmentStateRepository;

  @Inject
  ContactRepository contactRepository;

  @Inject
  FileService fileService;

  @Inject
  MongoTemplate mongoTemplate;

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
    saveContacts(study);
    mongoTemplate.save(study);
    updateRemovedContacts(study);

    return study;
  }

  @Override
  public void deleteWithReferences(Study study) {
    mongoTemplate.remove(study);
    deleteContacts(study);
  }

}
