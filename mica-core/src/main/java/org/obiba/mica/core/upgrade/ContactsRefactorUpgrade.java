package org.obiba.mica.core.upgrade;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.service.StudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;

@Component
public class ContactsRefactorUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(ContactsRefactorUpgrade.class);

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private StudyService studyService;

  @Inject
  private PersonRepository personRepository;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private NetworkService networkService;

  @Inject
  private EventBus eventBus;

  @Override
  public String getDescription() {
    return "Refactored attachments";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.0.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing contacts upgrade");
    studyRepository.findAll().forEach(study -> {
      study.getAllPersons().stream().forEach(p -> p.setEmail(Strings.emptyToNull(p.getEmail())));
      StudyState studyState = studyService.findStateById(study.getId());
      study.setContacts(replaceExistingPersons(study.getContacts()));
      study.setInvestigators(replaceExistingPersons(study.getInvestigators()));

      studyRepository.saveWithReferences(study);

      eventBus.post(new DraftStudyUpdatedEvent(study));
      study.getAllPersons().forEach(c -> eventBus.post(new PersonUpdatedEvent(c)));

      if(studyState.isPublished()) {
        eventBus.post(new StudyPublishedEvent(study, studyState.getPublishedBy()));
      }
    });

    networkRepository.findAll().forEach(network -> {
      network.getAllPersons().stream().forEach(p -> p.setEmail(Strings.emptyToNull(p.getEmail())));

      network.setContacts(replaceExistingPersons(network.getContacts()));
      network.setInvestigators(replaceExistingPersons(network.getInvestigators()));

      networkService.save(network, "System upgrade.");

      if(network.isPublished()) {
        networkService.publish(network.getId(), true);
      }
    });
  }

  private List<Person> replaceExistingPersons(List<Person> persons) {
    ImmutableList.copyOf(persons).forEach(c -> {
      if(c.getId() == null && !Strings.isNullOrEmpty(c.getEmail())) {
        Person person = personRepository.findOneByEmail(c.getEmail());

        if(person != null) {
          int idx = persons.indexOf(c);
          persons.remove(c);
          persons.add(idx, person);
        }
      }
    });

    return persons;
  }
}
