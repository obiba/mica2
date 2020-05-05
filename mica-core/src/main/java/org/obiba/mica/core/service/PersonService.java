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

import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.obiba.mica.contact.event.PersonDeletedEvent;
import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.StudyDeletedEvent;
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

  public List<Person> getStudyMemberships(String studyId) {
    return personRepository.findByStudyMemberships(studyId);
  }

  public List<Person> getNetworkMemberships(String networkId) {
    return personRepository.findByNetworkMemberships(networkId);
  }

  public Person save(Person person) {
    Person saved = personRepository.save(person);
    eventBus.post(new PersonUpdatedEvent(saved));
    return saved;
  }

  public void delete(String id) {
    Person personToDelete = findById(id);
    personRepository.delete(id);
    eventBus.post(new PersonDeletedEvent(personToDelete));
  }

  public Map<String, List<Membership>> getStudyMembershipMap(String studyId) {
    Map<String, List<Membership>> membershipMap = new HashMap<String, List<Membership>>();

    List<Person> studyMemberships = getStudyMemberships(studyId);
    studyMemberships.forEach(person -> {
      person.getStudyMemberships().stream()
        .filter(studyMembership -> studyMembership.getParentId().equals(studyId))
        .forEach(studyMembership -> {
          Membership membership = new Membership(person, studyMembership.getRole());
          if (!membershipMap.containsKey(studyMembership.getRole())) {
            membershipMap.put(studyMembership.getRole(), Lists.newArrayList(membership));
          } else {
            membershipMap.get(studyMembership.getRole()).add(membership);
          }
        });
    });

    return membershipMap;
  }

  public Map<String, List<Membership>> getNetworkMembershipMap(String networkId) {
    Map<String, List<Membership>> membershipMap = new HashMap<String, List<Membership>>();

    List<Person> studyMemberships = getNetworkMemberships(networkId);
    studyMemberships.forEach(person -> {
      person.getNetworkMemberships().stream()
        .filter(networkMembership -> networkMembership.getParentId().equals(networkId))
        .forEach(networkMembership -> {
          Membership membership = new Membership(person, networkMembership.getRole());
          if (!membershipMap.containsKey(networkMembership.getRole())) {
            membershipMap.put(networkMembership.getRole(), Lists.newArrayList(membership));
          } else {
            membershipMap.get(networkMembership.getRole()).add(membership);
          }
        });
    });

    return membershipMap;
  }

  public Map<String, List<Membership>> setMembershipOrder(Map<String, List<String>> membershipSortOrder, Map<String, List<Membership>> membershipMap) {
    membershipMap.forEach((role, people) -> {
      people.sort(new Comparator<Membership>() {
        @Override
        public int compare(Membership membership1, Membership membership2) {
          List<String> list = membershipSortOrder.get(role);
          if (list == null) return 0;
          return list.indexOf(membership1.getPerson().getId()) - list.indexOf(membership2.getPerson().getId());
        }
      });
    });

    return membershipMap;
  }

  @Async
  @Subscribe
  public void networkDeleted(NetworkDeletedEvent event) {
    Network network = event.getPersistable();
    List<Person> networkMembers = getNetworkMemberships(network.getId());
    networkMembers.forEach(person -> {
      person.removeNetwork(network);
      save(person);
    });
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    BaseStudy study = event.getPersistable();
    List<Person> studyMembers = getStudyMemberships(study.getId());
    studyMembers.forEach(person -> {
      person.removeStudy(study);
      save(person);
    });
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
