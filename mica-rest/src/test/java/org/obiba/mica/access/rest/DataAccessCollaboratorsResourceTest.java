/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessCollaboratorService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.security.service.SubjectAclService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataAccessCollaboratorsResourceTest {

  @InjectMocks
  private DataAccessCollaboratorsResource resource;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Mock
  private DataAccessRequestService dataAccessRequestService;

  @Mock
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  private final DataAccessConfig config = new DataAccessConfig();

  private DataAccessRequest request;

  @BeforeEach
  public void setUp() {
    resource.setParentId("dar1");
    request = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    request.setId("dar1");
    config.setCollaboratorsEnabled(true);
    when(dataAccessRequestUtilService.getDataAccessConfig()).thenReturn(config);
  }

  @Test
  public void test_invite_valid_email() {
    mockApplicant();

    Response response = resource.inviteCollaborator("o'brien+x@example.org");

    assertThat(response.getStatus()).isEqualTo(200);
    verify(dataAccessCollaboratorService).inviteCollaborator(request, "o'brien+x@example.org");
  }

  /**
   * The invited email is displayed as the collaborator name until the invitation is accepted (#4742).
   */
  @Test
  public void test_invite_invalid_email_is_bad_request() {
    mockApplicant();

    for (String email : new String[] { null, "", "bob", "<img src=x onerror=alert(1)>@x.org", "Bob <bob@x.org>" }) {
      assertThatThrownBy(() -> resource.inviteCollaborator(email)).isInstanceOf(BadRequestException.class);
    }

    verify(dataAccessCollaboratorService, never()).inviteCollaborator(any(), any());
  }

  @Test
  public void test_invite_when_collaborators_disabled_is_forbidden() {
    config.setCollaboratorsEnabled(false);

    Response response = resource.inviteCollaborator("bob@example.org");

    assertThat(response.getStatus()).isEqualTo(403);
    verifyNoInteractions(dataAccessRequestService, dataAccessCollaboratorService);
  }

  private void mockApplicant() {
    when(dataAccessRequestService.findById("dar1")).thenReturn(request);
    when(subjectAclService.isCurrentUser("applicant")).thenReturn(true);
  }

}
