/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.obiba.git.CommitInfo;
import org.obiba.mica.contact.event.PersonDeletedEvent;
import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Validated
public class PersonService {

  private static final Logger log = LoggerFactory.getLogger(PersonService.class);

  @Inject
  PersonRepository personRepository;

  @Inject
  protected GitService gitService;

  @Inject
  protected ObjectMapper objectMapper;

  @Inject
  EventBus eventBus;

  public Person findById(String id) {
    return personRepository.findById(id).orElse(null);
  }

  public Person getFromCommit(@NotNull Person gitPersistable, @NotNull String commitId) {
    String blob = gitService.getBlob(gitPersistable, commitId, getType());
    InputStream inputStream = new ByteArrayInputStream(blob.getBytes(StandardCharsets.UTF_8));

    try {
      return objectMapper.readValue(inputStream, getType());
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public List<Person> getStudyMemberships(String studyId) {
    return personRepository.findByStudyMemberships(studyId);
  }

  public List<Person> getNetworkMemberships(String networkId) {
    return personRepository.findByNetworkMemberships(networkId);
  }

  public Person save(Person person) {
    Person saved = person;

    if (!person.isNew()) {
      Optional<Person> found = personRepository.findById(person.getId());

      if (found.isPresent()) {
        saved = found.get();
        BeanUtils.copyProperties(person, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate");
      } else {
        saved = person;
      }
    }

    saved.setLastModifiedDate(LocalDateTime.now());

    personRepository.save(saved);

    eventBus.post(new PersonUpdatedEvent(saved));
    gitService.save(saved);
    return saved;
  }

  public void delete(String id) {
    Person personToDelete = findById(id);
    personRepository.deleteById(id);
    gitService.deleteGitRepository(personToDelete);
    eventBus.post(new PersonDeletedEvent(personToDelete));
  }

  public Iterable<CommitInfo> getCommitInfos(@NotNull Person persistable) {
    return gitService.getCommitsInfo(persistable, persistable.getClass());
  }

  public CommitInfo getCommitInfo(@NotNull Person persistable, @NotNull String commitInfo) {
    return gitService.getCommitInfo(persistable, commitInfo, persistable.getClass());
  }

  public Iterable<String> getDiffEntries(@NotNull Person persistable, @NotNull String commitId, @Nullable String prevCommitId) {
    return gitService.getDiffEntries(persistable, commitId, prevCommitId, persistable.getClass());
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

  protected Class<Person> getType() {
    return Person.class;
  }

  public String getTypeName() {
    return "person";
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
