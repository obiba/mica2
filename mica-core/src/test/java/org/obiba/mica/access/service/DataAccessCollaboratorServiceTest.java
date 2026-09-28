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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.access.DataAccessCollaboratorRepository;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessCollaboratorAcceptedEvent;
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
import static org.mockito.ArgumentMatchers.anyString;
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
