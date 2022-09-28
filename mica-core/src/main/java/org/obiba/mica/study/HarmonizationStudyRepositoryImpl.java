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
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.core.repository.PersonAwareRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class HarmonizationStudyRepositoryImpl implements HarmonizationStudyRepositoryCustom, PersonAwareRepository<HarmonizationStudy> {

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
  public List<Person> findAllPersonsByParent(HarmonizationStudy study) {
    return personRepository.findByStudyMemberships(study.getId());
  }

  @Override
  public HarmonizationStudy saveWithReferences(HarmonizationStudy study) {
    saveContacts(study);
    mongoTemplate.save(study);
    updateRemovedContacts(study);

    return study;
  }

  @Override
  public HarmonizationStudy insertWithReferences(HarmonizationStudy study) {
    saveContacts(study);
    mongoTemplate.insert    (study);
    updateRemovedContacts(study);

    return study;
  }

  @Override
  public void deleteWithReferences(HarmonizationStudy study) {
    mongoTemplate.remove(study);
    deleteContacts(study);
  }
}
