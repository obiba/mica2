/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.Set;

import javax.inject.Inject;

import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Service
@Validated
public class PersonService {

  private static final Logger log = LoggerFactory.getLogger(PersonService.class);

  @Inject
  PersonRepository personRepository;

  @Inject
  EventBus eventBus;

  public Person findById(String id) {
    return personRepository.findOne(id);
  }

  @Async
  @Subscribe
  public void micaConfigUpdated(MicaConfigUpdatedEvent event) {
    event.getRemovedRoles().forEach(r -> {
      log.info("Removing role {} from Persons.", r);
      Set<Person> persons = Sets.newHashSet(personRepository.findByStudyMembershipsRole(r));
      persons.addAll(personRepository.findByNetworkMembershipsRole(r));
      persons.forEach(p -> {
        p.removeAllMemberships(r);
        personRepository.save(p);
        eventBus.post(new PersonUpdatedEvent(p));
      });
    });
  }
}
