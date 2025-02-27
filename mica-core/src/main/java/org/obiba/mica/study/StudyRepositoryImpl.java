/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonAwareRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.List;

@Component
public class StudyRepositoryImpl implements StudyRepositoryCustom, PersonAwareRepository<Study> {

  @Inject
  PersonRepository personRepository;

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
  public Study insertWithReferences(Study study) {
    saveContacts(study);
    mongoTemplate.insert(study);
    updateRemovedContacts(study);

    return study;
  }

  @Override
  public void deleteWithReferences(Study study) {
    mongoTemplate.remove(study);
    deleteContacts(study);
  }
}
