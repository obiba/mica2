package org.obiba.mica.study;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.repository.PersonAwareRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

@Component
public class StudyRepositoryImpl implements StudyRepositoryCustom, PersonAwareRepository<Study> {

  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  AttachmentStateRepository attachmentStateRepository;

  @Inject
  PersonRepository personRepository;

  @Inject
  FileStoreService fileStoreService;

  @Inject
  MongoTemplate mongoTemplate;

  @Inject
  EventBus eventBus;

  @Override
  public PersonRepository getPersonRepository() {
    return personRepository;
  }

  @Override
  public EventBus getEventBus() {
    return eventBus;
  }

  @Override
  public List<Person> findAllPersonsByParent(Study study) {
    return personRepository.findByStudyMemberships(study.getId());
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
