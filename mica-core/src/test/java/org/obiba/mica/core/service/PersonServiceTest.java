/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.StudyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who can update the details of a person, and the reinstate filtering (#4753).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersonServiceTest {

  private static final String OWN_STUDY = "own";

  private static final String OTHER_STUDY = "other";

  private static final String DELETED_STUDY = "deleted";

  private static final String OWN_NETWORK = "net";

  @InjectMocks
  private PersonService personService;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private StudyRepository studyRepository;

  @Mock
  private HarmonizationStudyRepository harmonizationStudyRepository;

  @Mock
  private NetworkRepository networkRepository;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private GitService gitService;

  @Mock
  private EventBus eventBus;

  @BeforeEach
  public void setUp() {
    when(studyRepository.existsById(OWN_STUDY)).thenReturn(true);
    when(studyRepository.existsById(OTHER_STUDY)).thenReturn(true);
    when(networkRepository.existsById(OWN_NETWORK)).thenReturn(true);
    // scoped user: EDIT on its own study and network only
    when(subjectAclService.isPermitted("/draft/individual-study", "EDIT", OWN_STUDY)).thenReturn(true);
    when(subjectAclService.isPermitted("/draft/network", "EDIT", OWN_NETWORK)).thenReturn(true);
    when(micaConfigService.getRoles()).thenReturn(Lists.newArrayList("contact", "investigator"));
  }

  @Test
  public void global_editor_can_edit_any_person() {
    globalEdit();
    assertThat(personService.canEditDetails(person())).isTrue();
    assertThat(personService.canEditDetails(person(OTHER_STUDY))).isTrue();
  }

  @Test
  public void global_edit_requires_the_three_document_types() {
    when(subjectAclService.isPermitted("/draft/individual-study", "EDIT")).thenReturn(true);
    when(subjectAclService.isPermitted("/draft/harmonization-study", "EDIT")).thenReturn(true);
    assertThat(personService.hasGlobalEdit()).isFalse();
  }

  @Test
  public void scoped_user_can_edit_a_person_of_its_studies_and_networks_only() {
    Person person = person(OWN_STUDY);
    person.getNetworkMemberships().add(new Person.Membership(OWN_NETWORK, "contact"));
    assertThat(personService.canEditDetails(person)).isTrue();
  }

  @Test
  public void scoped_user_cannot_edit_a_person_also_member_of_another_study() {
    assertThat(personService.canEditDetails(person(OWN_STUDY, OTHER_STUDY))).isFalse();
  }

  @Test
  public void scoped_user_cannot_edit_an_orphan() {
    assertThat(personService.canEditDetails(person())).isFalse();
    // only deleted studies: an orphan too
    assertThat(personService.canEditDetails(person(DELETED_STUDY))).isFalse();
  }

  @Test
  public void membership_of_a_deleted_study_is_ignored() {
    assertThat(personService.canEditDetails(person(OWN_STUDY, DELETED_STUDY))).isTrue();
  }

  @Test
  public void reinstate_drops_memberships_of_missing_studies_and_unknown_roles() {
    Person stored = person(OWN_STUDY);
    stored.setId("p1");
    Person fromCommit = person(OWN_STUDY, DELETED_STUDY);
    fromCommit.getStudyMemberships().add(new Person.Membership(OTHER_STUDY, "removed-role"));
    when(personRepository.replace(any(Person.class), anyLong())).thenReturn(true);

    personService.reinstate(stored, fromCommit);

    assertThat(fromCommit.getId()).isEqualTo("p1");
    assertThat(fromCommit.getStudyMemberships()).containsExactly(new Person.Membership(OWN_STUDY, "contact"));
    // a document without version is guarded by 0
    verify(personRepository).replace(fromCommit, 0L);
  }

  @Test
  public void email_used_ignores_the_person_itself() {
    Person same = person();
    same.setId("p1");
    when(personRepository.findByEmailIgnoreCase("A@x.org")).thenReturn(List.of(same));
    assertThat(personService.isEmailUsed("A@x.org", "p1")).isFalse();
    assertThat(personService.isEmailUsed("A@x.org", null)).isTrue();
    assertThat(PersonService.sameEmail(" John@X.org", "john@x.org")).isTrue();
  }

  @Test
  public void stale_update_writes_no_history() {
    when(personRepository.updateDetails(eq("p1"), eq(3L), any(Person.class))).thenReturn(false);
    assertThat(personService.updateDetails("p1", 3L, person())).isNull();
    verify(gitService, never()).save(any());
    verify(personRepository, never()).findById(anyString());
  }

  @Test
  public void redundants_keep_the_oldest_and_skip_a_copy_changed_meanwhile() {
    Person oldest = copy("p1", 1);
    Person unchanged = copy("p2", 2);
    Person changed = copy("p3", 3);
    Person other = copy("p4", 0);
    other.getStudyMemberships().add(new Person.Membership(OTHER_STUDY, "contact"));
    when(personRepository.findAll()).thenReturn(Lists.newArrayList(changed, other, unchanged, oldest));
    when(personRepository.deleteIfUnchanged(unchanged)).thenReturn(true);
    when(personRepository.deleteIfUnchanged(changed)).thenReturn(false);

    assertThat(personService.deleteRedundants()).isEqualTo(1);
    verify(personRepository, never()).deleteIfUnchanged(oldest);
    verify(personRepository, never()).deleteIfUnchanged(other);
    verify(gitService).deleteGitRepository(unchanged);
    verify(gitService, never()).deleteGitRepository(changed);
  }

  /** the same person, created on day {@code day} of 2020 */
  private static Person copy(String id, int day) {
    Person person = person(OWN_STUDY);
    person.setId(id);
    person.setCreatedDate(java.time.LocalDateTime.of(2020, 1, day == 0 ? 10 : day, 0, 0));
    return person;
  }

  private void globalEdit() {
    when(subjectAclService.isPermitted("/draft/individual-study", "EDIT")).thenReturn(true);
    when(subjectAclService.isPermitted("/draft/harmonization-study", "EDIT")).thenReturn(true);
    when(subjectAclService.isPermitted("/draft/network", "EDIT")).thenReturn(true);
  }

  private static Person person(String... studyIds) {
    Person person = new Person();
    person.setLastName("Doe");
    for (String studyId : studyIds) person.getStudyMemberships().add(new Person.Membership(studyId, "contact"));
    return person;
  }
}
