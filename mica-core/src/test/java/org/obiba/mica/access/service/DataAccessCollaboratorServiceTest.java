/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.eventbus.EventBus;
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.access.DataAccessCollaboratorRepository;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessCollaboratorAcceptedEvent;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.realm.MicaAuthorizingRealm;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataAccessCollaboratorServiceTest {

  @InjectMocks
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  @Mock
  private DataAccessCollaboratorRepository dataAccessCollaboratorRepository;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private EventBus eventBus;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private UserProfileService userProfileService;

  @Mock
  private DataAccessConfigService dataAccessConfigService;

  @Mock
  private MicaAuthorizingRealm micaAuthorizingRealm;

  @Mock
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Mock
  private MailService mailService;

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void test_deleteAll_deletes_the_request_collaborators() {
    DataAccessCollaborator invited = DataAccessCollaborator.newBuilder("dar1").email("invited@example.org").invited().build();
    DataAccessCollaborator accepted = DataAccessCollaborator.newBuilder("dar1").email("accepted@example.org").build();
    accepted.setPrincipal("accepted");
    List<DataAccessCollaborator> collaborators = List.of(invited, accepted);
    when(dataAccessCollaboratorRepository.findByRequestId("dar1")).thenReturn(collaborators);

    dataAccessCollaboratorService.deleteAll("dar1");

    // collaborators are looked up by request, not by id (a collaborator's id is never the request's id)
    verify(dataAccessCollaboratorRepository).deleteAll(collaborators);
    verify(dataAccessCollaboratorRepository, never()).deleteById(anyString());
    // their permissions and agreements are removed with the request's ones
    verifyNoInteractions(subjectAclService, eventBus);
  }

  @Test
  public void test_acceptCollaborator_registers_the_invited_collaborator() throws Exception {
    DataAccessRequest dar = mockInvitation("dar1", "bob@example.org");
    DataAccessCollaborator invited = DataAccessCollaborator.newBuilder("dar1").email("bob@example.org").invited().build();
    when(dataAccessCollaboratorRepository.findByRequestIdAndEmail("dar1", "bob@example.org")).thenReturn(Optional.of(invited));

    dataAccessCollaboratorService.acceptCollaborator(dar, "key");

    assertThat(invited.isInvitationPending()).isFalse();
    assertThat(invited.getPrincipal()).isEqualTo("bob");
    verify(dataAccessCollaboratorRepository).save(invited);
    verify(subjectAclService).addUserPermission("bob", "/data-access-request", "VIEW", "dar1");
    verify(eventBus).post(any(DataAccessCollaboratorAcceptedEvent.class));
  }

  /**
   * No collaborator for the request and email: the invitation was removed, or its request was deleted and its
   * (incremental) id reused by a new request. The invitation must not give access to that request.
   */
  @Test
  public void test_acceptCollaborator_without_invitation_is_rejected() throws Exception {
    DataAccessRequest dar = mockInvitation("dar1", "bob@example.org");
    when(dataAccessCollaboratorRepository.findByRequestIdAndEmail("dar1", "bob@example.org")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> dataAccessCollaboratorService.acceptCollaborator(dar, "key"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("invitation-expired");

    verify(dataAccessCollaboratorRepository, never()).save(any());
    verifyNoInteractions(subjectAclService, eventBus, micaAuthorizingRealm);
  }

  // 64 + 1 + 189 characters
  private static final String LONGEST_EMAIL = "a".repeat(64) + "@" + "b".repeat(61) + "." + "c".repeat(61) + "."
    + "d".repeat(61) + ".org";

  /**
   * Same as Agate's EmailValidator.
   */
  @Test
  public void test_isValidEmail_accepts_addresses() {
    assertThat(LONGEST_EMAIL).hasSize(254);
    assertThat(List.of("bob@example.org", "bob.smith+mica@example.org", "o'brien@example.org", "a&b@example.org",
      "bob@localhost", "\"bob\"@example.org", "bob@[1.2.3.4]", "a".repeat(64) + "@example.org", LONGEST_EMAIL))
      .allMatch(DataAccessCollaboratorService::isValidEmail);
  }

  @Test
  public void test_isValidEmail_rejects_malformed_addresses() {
    assertThat(Arrays.asList(null, "", "   ", "bob", "@", "@@", "@example.org", "bob@", "bob@@example.org",
      "bob smith@example.org", " bob@example.org", "bob@example.org ", "bob@example.org\n", "bob@example.org'",
      "bob..smith@example.org", ".bob@example.org", "bob@example.org.",
      "a".repeat(65) + "@example.org", LONGEST_EMAIL.replace("@b", "@bb")))
      .noneMatch(DataAccessCollaboratorService::isValidEmail);
  }

  /**
   * InternetAddress parses more than a bare address, the stored email must be the address only and without markup.
   */
  @Test
  public void test_isValidEmail_rejects_markup_names_and_lists() {
    assertThat(List.of("<img src=x onerror=alert(1)>@x.org", "bob@<b>x</b>.org", "\"<img src=x>\"@x.org",
      "Bob <bob@x.org>", "Bob bob@x.org", "bob@x.org (Bob)", "bob@x.org, eve@x.org", "bob@x.org;eve@x.org",
      "bob@x.org?cc=eve@x.org"))
      .noneMatch(DataAccessCollaboratorService::isValidEmail);
  }

  @Test
  public void test_inviteCollaborator_saves_and_invites_valid_email() {
    DataAccessRequest dar = mockInviteCollaborator();
    when(dataAccessCollaboratorRepository.findByRequestIdAndEmail("dar1", "o'brien+x@example.org")).thenReturn(Optional.empty());

    dataAccessCollaboratorService.inviteCollaborator(dar, "o'brien+x@example.org");

    ArgumentCaptor<DataAccessCollaborator> saved = ArgumentCaptor.forClass(DataAccessCollaborator.class);
    verify(dataAccessCollaboratorRepository).save(saved.capture());
    // stored as typed
    assertThat(saved.getValue().getEmail()).isEqualTo("o'brien+x@example.org");
    assertThat(saved.getValue().isInvitationPending()).isTrue();
    verify(mailService).sendEmailToUsers("subject", "dataAccessRequestCollaboratorInvitation", Map.of("id", "dar1", "title", "Title", "key", "key"), "o'brien+x@example.org");
  }

  @Test
  public void test_inviteCollaborator_reinvites_existing_collaborator() {
    DataAccessRequest dar = mockInviteCollaborator();
    DataAccessCollaborator invited = DataAccessCollaborator.newBuilder("dar1").email("bob@example.org").invited().build();
    when(dataAccessCollaboratorRepository.findByRequestIdAndEmail("dar1", "bob@example.org")).thenReturn(Optional.of(invited));

    dataAccessCollaboratorService.inviteCollaborator(dar, "bob@example.org");

    verify(dataAccessCollaboratorRepository).save(invited);
    verify(mailService).sendEmailToUsers(anyString(), anyString(), anyMap(), eq("bob@example.org"));
  }

  /**
   * The invited email is displayed as the collaborator name until the invitation is accepted (#4742).
   */
  @Test
  public void test_inviteCollaborator_rejects_invalid_email() {
    DataAccessRequest dar = mockCollaboratorsConfig(true);

    assertThatThrownBy(() -> dataAccessCollaboratorService.inviteCollaborator(dar, "<img src=x onerror=alert(1)>@x.org"))
      .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(dataAccessCollaboratorRepository, mailService);
  }

  @Test
  public void test_inviteCollaborator_is_forbidden_when_collaborators_disabled() {
    DataAccessRequest dar = mockCollaboratorsConfig(false);

    assertThatThrownBy(() -> dataAccessCollaboratorService.inviteCollaborator(dar, "bob@example.org"))
      .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(dataAccessCollaboratorRepository, mailService);
  }

  private DataAccessRequest mockCollaboratorsConfig(boolean enabled) {
    DataAccessConfig config = new DataAccessConfig();
    config.setCollaboratorsEnabled(enabled);
    when(dataAccessRequestUtilService.getDataAccessConfig()).thenReturn(config);
    DataAccessRequest dar = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    dar.setId("dar1");
    return dar;
  }

  /**
   * Collaborators enabled, current user "applicant", and what the invitation email needs.
   */
  private DataAccessRequest mockInviteCollaborator() {
    DataAccessRequest dar = mockCollaboratorsConfig(true);
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("applicant");
    ThreadContext.bind(subject);
    when(dataAccessCollaboratorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(micaConfigService.encrypt(anyString())).thenReturn("key");
    when(dataAccessRequestUtilService.getNotificationEmailContext(dar)).thenReturn(new HashMap<>(Map.of("id", "dar1", "title", "Title")));
    when(dataAccessConfigService.getOrCreateConfig()).thenReturn(new DataAccessConfig());
    when(mailService.getSubject(any(), any(), any())).thenReturn("subject");
    return dar;
  }

  /**
   * A valid, unexpired invitation key to the request, for user "bob" whose profile has the invited email.
   */
  private DataAccessRequest mockInvitation(String requestId, String email) throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("bob");
    ThreadContext.bind(subject);

    JSONObject key = new JSONObject();
    key.put("author", "applicant");
    key.put("request", requestId);
    key.put("email", email);
    key.put("created", LocalDateTime.now().toString());
    when(micaConfigService.decrypt("key")).thenReturn(key.toString());

    ObibaRealm.Subject profile = new ObibaRealm.Subject();
    profile.setAttributes(List.of(Map.of("key", "email", "value", email)));
    when(userProfileService.getProfile("bob", true)).thenReturn(profile);

    DataAccessConfig config = new DataAccessConfig();
    config.setNotifyCollaboratorAccepted(false);
    when(dataAccessConfigService.getOrCreateConfig()).thenReturn(config);

    DataAccessRequest dar = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    dar.setId(requestId);
    return dar;
  }

}
