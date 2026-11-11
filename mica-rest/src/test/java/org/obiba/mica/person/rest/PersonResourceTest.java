/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.rest;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.service.EntityConfigKeyTranslationService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.obiba.web.model.ErrorDtos.ClientErrorDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The person details update and the membership endpoints (#4753).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersonResourceTest {

  private static final String ID = "p1";

  private PersonResource resource;

  @Mock
  private Dtos dtos;

  @Mock
  private PersonService personService;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private StudyService studyService;

  @Mock
  private NetworkService networkService;

  @Mock
  private EntityConfigKeyTranslationService entityConfigKeyTranslationService;

  @Mock
  private MicaConfigService micaConfigService;

  private Person stored;

  @BeforeEach
  public void setUp() {
    Validator validator = Validation.byDefaultProvider().configure()
      .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory().getValidator();
    resource = new PersonResource(dtos, personService, subjectAclService, studyService, networkService,
      entityConfigKeyTranslationService, micaConfigService, validator);

    stored = person("Doe", "doe@x.org");
    stored.setId(ID);
    when(personService.findById(ID)).thenReturn(stored);
    when(personService.canEditDetails(stored)).thenReturn(true);
    when(dtos.fromDto(any(PersonDto.class))).thenAnswer(invocation -> {
      PersonDto dto = invocation.getArgument(0);
      return person(dto.getLastName(), dto.hasEmail() ? dto.getEmail() : null);
    });
    when(dtos.asDto(any(Person.class), eq(true))).thenReturn(PersonDto.newBuilder().setLastName("Doe").build());
    when(micaConfigService.getRoles()).thenReturn(Lists.newArrayList("contact", "investigator"));
  }

  @Test
  public void update_needs_edit_rights_on_the_stored_person() {
    when(personService.canEditDetails(stored)).thenReturn(false);
    assertThatThrownBy(() -> resource.update(ID, dto("Doe").setVersion(0).build()))
      .isInstanceOf(ForbiddenException.class);
    verify(personService, never()).updateDetails(anyString(), anyLong(), any());
  }

  @Test
  public void update_of_a_missing_person_is_not_found() {
    assertThatThrownBy(() -> resource.update("missing", dto("Doe").setVersion(0).build()))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void update_requires_the_version() {
    assertError(() -> resource.update(ID, dto("Doe").build()), 400, PersonErrors.VERSION_MISSING);
  }

  @Test
  public void update_rejects_another_id() {
    assertError(() -> resource.update(ID, dto("Doe").setId("p2").setVersion(0).build()), 400, PersonErrors.ID_MISMATCH);
  }

  @Test
  public void update_rejects_a_blank_last_name() {
    assertError(() -> resource.update(ID, dto("").setVersion(0).build()), 400, PersonErrors.INVALID);
  }

  @Test
  public void stale_version_is_a_conflict() {
    when(personService.updateDetails(eq(ID), eq(3L), any(Person.class))).thenReturn(null);
    assertError(() -> resource.update(ID, dto("Doe").setVersion(3).build()), 409, PersonErrors.VERSION_CONFLICT);
  }

  @Test
  public void email_of_another_person_is_a_conflict() {
    when(personService.isEmailUsed("other@x.org", ID)).thenReturn(true);
    assertError(() -> resource.update(ID, dto("Doe").setEmail("other@x.org").setVersion(0).build()), 409,
      PersonErrors.EMAIL_USED);
  }

  @Test
  public void email_case_change_is_not_checked() {
    when(personService.isEmailUsed(anyString(), anyString())).thenReturn(true);
    when(personService.updateDetails(eq(ID), eq(0L), any(Person.class))).thenReturn(stored);

    resource.update(ID, dto("Doe").setEmail(" DOE@x.org").setVersion(0).build());

    verify(personService, never()).isEmailUsed(anyString(), anyString());
  }

  @Test
  public void update_writes_the_details_with_the_version() {
    when(personService.updateDetails(eq(ID), eq(2L), any(Person.class))).thenReturn(stored);
    PersonDto body = dto("Smith").setVersion(2)
      .addStudyMemberships(PersonDto.MembershipDto.newBuilder().setParentId("own").setRole("contact"))
      .build();

    resource.update(ID, body);

    verify(personService).updateDetails(eq(ID), eq(2L), any(Person.class));
    verify(personService, never()).addStudyMembership(anyString(), anyString(), anyString());
  }

  @Test
  public void link_to_an_unknown_study_is_not_found() {
    when(studyService.findStudy("unknown")).thenThrow(NoSuchEntityException.withId(Study.class, "unknown"));
    assertThatThrownBy(() -> resource.updatePersonRoleForStudy(ID, "unknown", "contact"))
      .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void link_with_an_unknown_role_is_a_bad_request() {
    Study study = new Study();
    study.setId("s1");
    when(studyService.findStudy("s1")).thenReturn(study);
    assertError(() -> resource.updatePersonRoleForStudy(ID, "s1", "unknown"), 400, PersonErrors.INVALID_ROLE);
  }

  @Test
  public void link_checks_the_study_and_adds_one_membership() {
    Study study = new Study();
    study.setId("s1");
    when(studyService.findStudy("s1")).thenReturn(study);
    when(personService.addStudyMembership(ID, "s1", "contact")).thenReturn(stored);

    resource.updatePersonRoleForStudy(ID, "s1", "contact");

    verify(subjectAclService).checkPermission("/draft/individual-study", "EDIT", "s1");
    verify(personService).addStudyMembership(ID, "s1", "contact");
  }

  @Test
  public void unlink_of_a_missing_person_is_not_found() {
    when(personService.studyResource("s1")).thenReturn("/draft/individual-study");
    assertThatThrownBy(() -> resource.removePersonRoleFromStudy("missing", "s1", "contact"))
      .isInstanceOf(NotFoundException.class);
    verify(subjectAclService).checkPermission("/draft/individual-study", "EDIT", "s1");
  }

  @Test
  public void get_tells_whether_the_caller_can_edit() {
    when(personService.canEditDetails(stored)).thenReturn(false);
    assertThat(resource.get(ID).getCanEdit()).isFalse();
    when(personService.canEditDetails(stored)).thenReturn(true);
    assertThat(resource.get(ID).getCanEdit()).isTrue();
  }

  @Test
  public void delete_and_restore_are_for_administrators_only() throws NoSuchMethodException {
    for (Method method : new Method[] {
      PersonResource.class.getMethod("delete", String.class),
      PersonResource.class.getMethod("restoreCommit", String.class, String.class) }) {
      assertThat(method.getAnnotation(RequiresRoles.class).value()).containsExactly(Roles.MICA_ADMIN);
      assertThat(method.getAnnotation(RequiresPermissions.class)).isNull();
    }
  }

  @Test
  public void restore_of_a_changed_person_is_a_conflict() throws Exception {
    Person fromCommit = person("Old", null);
    when(personService.getFromCommit(stored, "c1")).thenReturn(fromCommit);
    when(personService.reinstate(stored, fromCommit)).thenReturn(null);
    assertError(() -> resource.restoreCommit(ID, "c1"), 409, PersonErrors.VERSION_CONFLICT);
  }

  @Test
  public void restore_of_an_email_used_by_another_person_is_a_conflict() throws Exception {
    Person fromCommit = person("Old", "john@x.org");
    when(personService.getFromCommit(stored, "c1")).thenReturn(fromCommit);
    when(personService.isEmailUsed("john@x.org", ID)).thenReturn(true);
    assertError(() -> resource.restoreCommit(ID, "c1"), 409, PersonErrors.EMAIL_USED);
    verify(personService, never()).reinstate(any(), any());
  }

  private static PersonDto.Builder dto(String lastName) {
    return PersonDto.newBuilder().setLastName(lastName);
  }

  private static Person person(String lastName, String email) {
    Person person = new Person();
    person.setLastName(lastName);
    person.setEmail(email);
    return person;
  }

  /** the status and a ClientErrorDto with the message template, as JSON */
  private static void assertError(ThrowingCall call, int status, String template) {
    assertThatThrownBy(call::call).isInstanceOfSatisfying(WebApplicationException.class, e -> {
      assertThat(e.getResponse().getStatus()).isEqualTo(status);
      assertThat(e.getResponse().getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      ClientErrorDto error = (ClientErrorDto) e.getResponse().getEntity();
      assertThat(error.getCode()).isEqualTo(status);
      assertThat(error.getMessageTemplate()).isEqualTo(template);
      assertThat(error.getMessage()).isNotBlank();
    });
  }

  @FunctionalInterface
  interface ThrowingCall {
    Object call() throws Exception;
  }
}
