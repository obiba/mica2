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

import com.fasterxml.jackson.databind.JsonNode;
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
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A person is shared by all the studies and networks it is a member of: each write changes only what it is about
 * (the details, or one membership), never the whole document, except a reinstate.
 */
@Service
@Validated
public class PersonService {

  private static final Logger log = LoggerFactory.getLogger(PersonService.class);

  public static final String INDIVIDUAL_STUDY_RESOURCE = "/draft/individual-study";

  public static final String HARMONIZATION_STUDY_RESOURCE = "/draft/harmonization-study";

  public static final String NETWORK_RESOURCE = "/draft/network";

  private static final String EDIT = "EDIT";

  /** a stored membership whose study or network exists, with the ACL resource of that study or network */
  public record Link(String resource, String parentId, String role) {}

  @Inject
  PersonRepository personRepository;

  @Inject
  StudyRepository studyRepository;

  @Inject
  HarmonizationStudyRepository harmonizationStudyRepository;

  @Inject
  NetworkRepository networkRepository;

  @Inject
  SubjectAclService subjectAclService;

  @Inject
  MicaConfigService micaConfigService;

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

  //
  // Permissions
  //

  /** EDIT on all the individual studies, harmonization studies and networks (administrator, reviewer, editor) */
  public boolean hasGlobalEdit() {
    return subjectAclService.isPermitted(INDIVIDUAL_STUDY_RESOURCE, EDIT)
      && subjectAclService.isPermitted(HARMONIZATION_STUDY_RESOURCE, EDIT)
      && subjectAclService.isPermitted(NETWORK_RESOURCE, EDIT);
  }

  /** the stored memberships, all roles, whose study or network still exists */
  public List<Link> existingLinks(@NotNull Person stored) {
    List<Link> links = new ArrayList<>();
    stored.getStudyMemberships().forEach(m -> {
      String resource = studyResource(m.getParentId());
      if (resource != null) links.add(new Link(resource, m.getParentId(), m.getRole()));
    });
    stored.getNetworkMemberships().forEach(m -> {
      if (networkExists(m.getParentId())) links.add(new Link(NETWORK_RESOURCE, m.getParentId(), m.getRole()));
    });
    return links;
  }

  /** global edit, or EDIT on every study and network the stored person is a member of (at least one) */
  public boolean canEditDetails(@NotNull Person stored) {
    if (hasGlobalEdit()) return true;
    List<Link> links = existingLinks(stored);
    return !links.isEmpty() && links.stream()
      .allMatch(link -> subjectAclService.isPermitted(link.resource(), EDIT, link.parentId()));
  }

  /** the ACL resource of the study, null when there is no such study */
  @Nullable
  public String studyResource(String studyId) {
    if (studyId == null) return null;
    if (studyRepository.existsById(studyId)) return INDIVIDUAL_STUDY_RESOURCE;
    if (harmonizationStudyRepository.existsById(studyId)) return HARMONIZATION_STUDY_RESOURCE;
    return null;
  }

  public boolean networkExists(String networkId) {
    return networkId != null && networkRepository.existsById(networkId);
  }

  /** another person with this email, trimmed and ignoring case; never for an empty email */
  public boolean isEmailUsed(@Nullable String email, @Nullable String exceptId) {
    return personRepository.findByEmailIgnoreCase(email).stream().anyMatch(p -> !p.getId().equals(exceptId));
  }

  /** same email, trimmed and ignoring case */
  public static boolean sameEmail(@Nullable String email1, @Nullable String email2) {
    return normalizeEmail(email1).equals(normalizeEmail(email2));
  }

  private static String normalizeEmail(@Nullable String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }

  //
  // Writes
  //

  /** inserts the new person (no id), version 0 */
  public Person create(@NotNull Person person) {
    if (person.getId() != null) throw new IllegalArgumentException("A new person has no id");
    person.setVersion(0L);
    person.setLastModifiedDate(LocalDateTime.now());
    Person inserted = personRepository.insert(person);
    return afterWrite(inserted.getId());
  }

  /**
   * Replaces the details (not the memberships) when the stored version is {@code version}.
   *
   * @return the updated person, null when the person is missing or changed meanwhile
   */
  @Nullable
  public Person updateDetails(@NotNull String id, long version, @NotNull Person details) {
    return personRepository.updateDetails(id, version, details) ? afterWrite(id) : null;
  }

  /** @return the updated person, null when the person is missing */
  @Nullable
  public Person addStudyMembership(@NotNull String id, @NotNull String studyId, @NotNull String role) {
    return personRepository.addStudyMembership(id, studyId, role) ? afterWrite(id) : null;
  }

  /** @return the updated person, null when the person is missing */
  @Nullable
  public Person removeStudyMembership(@NotNull String id, @NotNull String studyId, @NotNull String role) {
    return personRepository.removeStudyMembership(id, studyId, role) ? afterWrite(id) : null;
  }

  /** @return the updated person, null when the person is missing */
  @Nullable
  public Person addNetworkMembership(@NotNull String id, @NotNull String networkId, @NotNull String role) {
    return personRepository.addNetworkMembership(id, networkId, role) ? afterWrite(id) : null;
  }

  /** @return the updated person, null when the person is missing */
  @Nullable
  public Person removeNetworkMembership(@NotNull String id, @NotNull String networkId, @NotNull String role) {
    return personRepository.removeNetworkMembership(id, networkId, role) ? afterWrite(id) : null;
  }

  /**
   * Restores the content of a commit, memberships included, except those of a missing study or network or of a
   * role that is not configured. Guarded by the stored version.
   *
   * @return the restored person, null when the person changed meanwhile
   */
  @Nullable
  public Person reinstate(@NotNull Person stored, @NotNull Person fromCommit) {
    List<String> roles = micaConfigService.getRoles();
    fromCommit.setId(stored.getId());
    fromCommit.setStudyMemberships(fromCommit.getStudyMemberships().stream()
      .filter(m -> roles.contains(m.getRole()) && studyResource(m.getParentId()) != null)
      .distinct().collect(Collectors.toList()));
    fromCommit.setNetworkMemberships(fromCommit.getNetworkMemberships().stream()
      .filter(m -> roles.contains(m.getRole()) && networkExists(m.getParentId()))
      .distinct().collect(Collectors.toList()));
    return personRepository.replace(fromCommit, stored.getVersionValue()) ? afterWrite(stored.getId()) : null;
  }

  public void delete(String id) {
    Person personToDelete = findById(id);
    personRepository.deleteById(id);
    gitService.deleteGitRepository(personToDelete);
    eventBus.post(new PersonDeletedEvent(personToDelete));
  }

  /**
   * Deletes the persons whose stored document is the same as another one (all the fields and memberships, ignoring
   * id, timestamps and version), keeping the oldest of each group.
   *
   * @return the number of persons deleted
   */
  public int deleteRedundants() {
    Map<List<Object>, List<Person>> groups = new LinkedHashMap<>();
    personRepository.findAll().forEach(p -> groups.computeIfAbsent(signature(p), k -> new ArrayList<>()).add(p));
    int count = 0;
    for (List<Person> group : groups.values()) {
      if (group.size() < 2) continue;
      group.sort(Comparator.comparing((Person p) -> p.getCreatedDate().orElse(LocalDateTime.MAX))
        .thenComparing(Person::getId));
      for (Person redundant : group.subList(1, group.size())) {
        // not deleted when written since read (e.g. linked to a study meanwhile): it may no longer be a copy
        if (personRepository.deleteIfUnchanged(redundant)) {
          log.info("Deleted person {}, same as {}", redundant.getId(), group.get(0).getId());
          gitService.deleteGitRepository(redundant);
          eventBus.post(new PersonDeletedEvent(redundant));
          count++;
        } else {
          log.info("Person {} changed meanwhile, not deleted", redundant.getId());
        }
      }
    }
    return count;
  }

  private List<Object> signature(Person person) {
    JsonNode institution = person.getInstitution() == null ? null : objectMapper.valueToTree(person.getInstitution());
    return Lists.newArrayList(person.getTitle(), person.getFirstName(), person.getLastName(), person.getAcademicLevel(),
      person.getEmail(), person.getPhone(), person.isDataAccessCommitteeMember(), institution,
      Sets.newHashSet(person.getStudyMemberships()), Sets.newHashSet(person.getNetworkMemberships()));
  }

  /** the stored person, with its history and index updated */
  private Person afterWrite(String id) {
    Person reRead = findById(id);
    if (reRead == null) return null; // deleted meanwhile
    gitService.save(reRead);
    eventBus.post(new PersonUpdatedEvent(reRead));
    return reRead;
  }

  //
  // History
  //

  public Iterable<CommitInfo> getCommitInfos(@NotNull Person persistable) {
    return gitService.getCommitsInfo(persistable, persistable.getClass());
  }

  public CommitInfo getCommitInfo(@NotNull Person persistable, @NotNull String commitInfo) {
    return gitService.getCommitInfo(persistable, commitInfo, persistable.getClass());
  }

  public Iterable<String> getDiffEntries(@NotNull Person persistable, @NotNull String commitId, @Nullable String prevCommitId) {
    return gitService.getDiffEntries(persistable, commitId, prevCommitId, persistable.getClass());
  }

  //
  // Memberships of a study or network
  //

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

  //
  // Events
  //

  @Async
  @Subscribe
  public void networkDeleted(NetworkDeletedEvent event) {
    personRepository.removeAllNetworkMemberships(event.getPersistable().getId()).forEach(this::afterWrite);
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    personRepository.removeAllStudyMemberships(event.getPersistable().getId()).forEach(this::afterWrite);
  }

  @Async
  @Subscribe
  public void micaConfigUpdated(MicaConfigUpdatedEvent event) {
    event.getRemovedRoles().forEach(r -> {
      log.info("Removing role {} from Persons.", r);
      personRepository.removeAllMembershipsWithRole(r).forEach(this::afterWrite);
    });
  }
}
