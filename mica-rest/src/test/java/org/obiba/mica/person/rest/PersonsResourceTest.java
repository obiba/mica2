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

import jakarta.ws.rs.WebApplicationException;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica.PersonDto;
import org.obiba.web.model.ErrorDtos.ClientErrorDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Person creation (#4753).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersonsResourceTest {

  private PersonsResource resource;

  @Mock
  private Dtos dtos;

  @Mock
  private PersonService personService;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private StudyService studyService;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private EventBus eventBus;

  @BeforeEach
  public void setUp() {
    resource = new PersonsResource(dtos, personService, subjectAclService, studyService, micaConfigService, eventBus);
    when(micaConfigService.getRoles()).thenReturn(Lists.newArrayList("contact", "investigator"));
    when(personService.studyResource("s1")).thenReturn(PersonService.INDIVIDUAL_STUDY_RESOURCE);
    when(personService.networkExists("n1")).thenReturn(true);
    when(dtos.fromDto(any(PersonDto.class))).thenReturn(new Person());
    when(personService.create(any(Person.class))).thenReturn(new Person());
    when(dtos.asDto(any(Person.class), eq(true))).thenReturn(PersonDto.newBuilder().setLastName("Doe").build());
  }

  @Test
  public void create_rejects_any_id_even_empty() {
    assertError(() -> resource.createPerson(dto().setId("").build()), 400, PersonErrors.ID_NOT_ALLOWED);
    assertError(() -> resource.createPersonStrict(dto().setId("p1").addStudyMemberships(study("s1", "contact")).build()),
      400, PersonErrors.ID_NOT_ALLOWED);
    verify(personService, never()).create(any());
  }

  @Test
  public void create_rejects_a_used_email() {
    when(personService.isEmailUsed("John@X.org", null)).thenReturn(true);
    assertError(() -> resource.createPerson(dto().setEmail("John@X.org").build()), 409, PersonErrors.EMAIL_USED);
    assertError(() -> resource.createPersonStrict(dto().setEmail("John@X.org")
      .addStudyMemberships(study("s1", "contact")).build()), 409, PersonErrors.EMAIL_USED_LINK_EXISTING);
  }

  @Test
  public void strict_create_without_membership_needs_global_edit() {
    assertError(() -> resource.createPersonStrict(dto().build()), 400, PersonErrors.MEMBERSHIP_REQUIRED);

    when(personService.hasGlobalEdit()).thenReturn(true);
    resource.createPersonStrict(dto().build());
    verify(personService).create(any(Person.class));
  }

  @Test
  public void strict_create_validates_roles_and_parents() {
    assertError(() -> resource.createPersonStrict(dto().addStudyMemberships(study("s1", "unknown")).build()),
      400, PersonErrors.INVALID_ROLE);
    assertError(() -> resource.createPersonStrict(dto().addStudyMemberships(study("missing", "contact")).build()),
      400, PersonErrors.NO_SUCH_STUDY);
    assertError(() -> resource.createPersonStrict(dto().addNetworkMemberships(study("missing", "contact")).build()),
      400, PersonErrors.NO_SUCH_NETWORK);
    verify(personService, never()).create(any());
  }

  @Test
  public void strict_create_needs_edit_on_each_parent() {
    doThrow(new AuthorizationException()).when(subjectAclService).checkPermission("/draft/network", "EDIT", "n1");
    assertThatThrownBy(() -> resource.createPersonStrict(dto()
      .addStudyMemberships(study("s1", "contact"))
      .addNetworkMemberships(study("n1", "contact")).build()))
      .isInstanceOf(AuthorizationException.class);
    verify(subjectAclService).checkPermission("/draft/individual-study", "EDIT", "s1");
    verify(personService, never()).create(any());
  }

  @Test
  public void remove_redundants_is_for_administrators_only() throws NoSuchMethodException {
    assertThat(PersonsResource.class.getMethod("deleteRedundants").getAnnotation(RequiresRoles.class).value())
      .containsExactly(Roles.MICA_ADMIN);
  }

  private static PersonDto.Builder dto() {
    return PersonDto.newBuilder().setLastName("Doe");
  }

  private static PersonDto.MembershipDto study(String parentId, String role) {
    return PersonDto.MembershipDto.newBuilder().setParentId(parentId).setRole(role).build();
  }

  /** the status and a ClientErrorDto with the message template */
  private static void assertError(Runnable call, int status, String template) {
    assertThatThrownBy(call::run).isInstanceOfSatisfying(WebApplicationException.class, e -> {
      assertThat(e.getResponse().getStatus()).isEqualTo(status);
      assertThat(((ClientErrorDto) e.getResponse().getEntity()).getMessageTemplate()).isEqualTo(template);
    });
  }
}
