/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.rest;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.security.PermissionsUtils;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubjectAclResourceTest {

  private static final String RESOURCE = "/draft/individual-study";

  private static final String INSTANCE = "s1";

  private static final String FILE_RESOURCE = "/draft/file";

  private static final String FILE_INSTANCE = "/individual-study/s1";

  private static final String READER = PermissionsUtils.getReaderActions();

  private static final String EDITOR = PermissionsUtils.getEditorActions();

  private static final String REVIEWER = PermissionsUtils.getReviewerActions();

  private static final SubjectAcl.Type USER = SubjectAcl.Type.USER;

  @InjectMocks
  private SubjectAclResource resource;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private UserProfileService userProfileService;

  @BeforeEach
  public void setUp() {
    resource.setResourceInstance(RESOURCE, INSTANCE);
    resource.setFileResourceInstance(FILE_RESOURCE, FILE_INSTANCE);
    lenient().when(subjectAclService.findSubjectAcl(any(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    lenient().when(userProfileService.userHas(anyString(), eq(Roles.MICA_EXTERNAL_EDITOR))).thenReturn(true);
  }

  @Test
  public void test_transfer_to_non_external_editor_is_bad_request() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    when(userProfileService.userHas("bob", Roles.MICA_EXTERNAL_EDITOR)).thenReturn(false);

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();
  }

  @Test
  public void test_transfer_between_groups_is_bad_request() {
    assertThatThrownBy(() -> resource.transfer("alice", "bob", "GROUP", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();
  }

  @Test
  public void test_transfer_on_all_documents_of_a_type_is_bad_request() {
    resource.setResourceInstance(RESOURCE, "*");

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(subjectAclService);
  }

  @Test
  public void test_transfer_moves_instance_and_file_acls_keeping_actions() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    given("alice", FILE_RESOURCE, FILE_INSTANCE, EDITOR);

    Response response = resource.transfer("alice", "bob", "USER", null);

    assertThat(response.getStatus()).isEqualTo(204);
    InOrder order = inOrder(subjectAclService);
    order.verify(subjectAclService).checkPermission(RESOURCE, "EDIT", INSTANCE);
    order.verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, EDITOR, INSTANCE);
    order.verify(subjectAclService).addSubjectPermission(USER, "bob", FILE_RESOURCE, EDITOR, FILE_INSTANCE);
    order.verify(subjectAclService).removeSubjectPermissions(USER, "alice", RESOURCE, INSTANCE);
    order.verify(subjectAclService).removeSubjectPermissions(USER, "alice", FILE_RESOURCE, FILE_INSTANCE);
  }

  /**
   * Without a role, the actions are copied as they are, per resource: not mapped to a role and back (which would
   * drop DELETE here), nor the document actions applied to the files (which would raise the file access).
   */
  @Test
  public void test_transfer_copies_the_actions_of_each_resource() {
    given("alice", RESOURCE, INSTANCE, "ADD,VIEW,EDIT,DELETE");
    given("alice", FILE_RESOURCE, FILE_INSTANCE, READER);

    resource.transfer("alice", "bob", "USER", null);

    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, "ADD,VIEW,EDIT,DELETE", INSTANCE);
    verify(subjectAclService).addSubjectPermission(USER, "bob", FILE_RESOURCE, READER, FILE_INSTANCE);
  }

  @Test
  public void test_transfer_with_role_and_no_file_acl() {
    given("alice", RESOURCE, INSTANCE, EDITOR);

    resource.transfer("alice", "bob", "USER", "reviewer");

    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, REVIEWER, INSTANCE);
    verify(subjectAclService, never()).addSubjectPermission(any(), anyString(), eq(FILE_RESOURCE), anyString(), anyString());
    verify(subjectAclService).removeSubjectPermissions(USER, "alice", RESOURCE, INSTANCE);
    verify(subjectAclService, never()).removeSubjectPermissions(USER, "alice", FILE_RESOURCE, FILE_INSTANCE);
  }

  @Test
  public void test_transfer_with_role_applies_to_files() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    given("alice", FILE_RESOURCE, FILE_INSTANCE, READER);

    resource.transfer("alice", "bob", "USER", "reader");

    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, READER, INSTANCE);
    verify(subjectAclService).addSubjectPermission(USER, "bob", FILE_RESOURCE, READER, FILE_INSTANCE);
  }

  /**
   * ADMINISTRATOR has no actions, which grants all of them, and ANALYST is not a document role.
   */
  @Test
  public void test_transfer_with_non_document_role_is_bad_request() {
    lenient().when(subjectAclService.findSubjectAcl(USER, "alice", RESOURCE, INSTANCE)).thenReturn(Optional.of(acl("alice", RESOURCE, EDITOR)));

    for (String role : new String[] { "administrator", "ADMINISTRATOR", "analyst", "owner", "" }) {
      assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", role)).as(role).isInstanceOf(BadRequestException.class);
    }
    verifyNoWrites();
  }

  /**
   * A permission without actions grants all of them: it is not handed over unless a role is given.
   */
  @Test
  public void test_transfer_permission_without_actions_requires_a_role() {
    SubjectAcl all = SubjectAcl.newBuilder("alice", USER).resource(RESOURCE).instance(INSTANCE).build();
    when(subjectAclService.findSubjectAcl(USER, "alice", RESOURCE, INSTANCE)).thenReturn(Optional.of(all));

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();

    resource.transfer("alice", "bob", "USER", "editor");
    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, EDITOR, INSTANCE);
  }

  @Test
  public void test_transfer_file_permission_without_actions_requires_a_role() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    SubjectAcl all = SubjectAcl.newBuilder("alice", USER).resource(FILE_RESOURCE).instance(FILE_INSTANCE).build();
    when(subjectAclService.findSubjectAcl(USER, "alice", FILE_RESOURCE, FILE_INSTANCE)).thenReturn(Optional.of(all));

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();
  }

  /**
   * A reader of the document who becomes its owner is raised to the transferred role, on the document and its files.
   */
  @Test
  public void test_transfer_raises_the_permissions_of_to() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    given("alice", FILE_RESOURCE, FILE_INSTANCE, EDITOR);
    given("bob", RESOURCE, INSTANCE, READER);
    given("bob", FILE_RESOURCE, FILE_INSTANCE, READER);

    resource.transfer("alice", "bob", "USER", "editor");

    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, "VIEW,ADD,EDIT", INSTANCE);
    verify(subjectAclService).addSubjectPermission(USER, "bob", FILE_RESOURCE, "VIEW,ADD,EDIT", FILE_INSTANCE);
    assertThat(PermissionsUtils.asRole(List.of("VIEW", "ADD", "EDIT"))).isEqualTo("EDITOR");
    verify(subjectAclService).removeSubjectPermissions(USER, "alice", RESOURCE, INSTANCE);
    verify(subjectAclService).removeSubjectPermissions(USER, "alice", FILE_RESOURCE, FILE_INSTANCE);
  }

  /**
   * The transfer never lowers a permission of {@code to}.
   */
  @Test
  public void test_transfer_keeps_the_higher_permission_of_to() {
    given("alice", RESOURCE, INSTANCE, READER);
    given("bob", RESOURCE, INSTANCE, REVIEWER);

    resource.transfer("alice", "bob", "USER", null);

    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, REVIEWER, INSTANCE);
    verify(subjectAclService).removeSubjectPermissions(USER, "alice", RESOURCE, INSTANCE);
  }

  /**
   * A permission of {@code to} granting all actions is not touched, the one of {@code from} is still removed.
   */
  @Test
  public void test_transfer_keeps_a_permission_of_to_without_actions() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    SubjectAcl all = SubjectAcl.newBuilder("bob", USER).resource(RESOURCE).instance(INSTANCE).build();
    when(subjectAclService.findSubjectAcl(USER, "bob", RESOURCE, INSTANCE)).thenReturn(Optional.of(all));

    resource.transfer("alice", "bob", "USER", null);

    verify(subjectAclService, never()).addSubjectPermission(any(), anyString(), anyString(), anyString(), anyString());
    verify(subjectAclService).removeSubjectPermissions(USER, "alice", RESOURCE, INSTANCE);
  }

  /**
   * Where {@code from} has no permission, nothing is transferred: the file permission of {@code to} is left as is.
   */
  @Test
  public void test_transfer_leaves_the_other_permissions_of_to() {
    given("alice", RESOURCE, INSTANCE, EDITOR);
    lenient().when(subjectAclService.findSubjectAcl(USER, "bob", FILE_RESOURCE, FILE_INSTANCE))
      .thenReturn(Optional.of(acl("bob", FILE_RESOURCE, READER)));

    resource.transfer("alice", "bob", "USER", null);

    verify(subjectAclService).addSubjectPermission(USER, "bob", RESOURCE, EDITOR, INSTANCE);
    verify(subjectAclService, never()).addSubjectPermission(any(), anyString(), eq(FILE_RESOURCE), anyString(), anyString());
    verify(subjectAclService, never()).removeSubjectPermissions(any(), anyString(), eq(FILE_RESOURCE), anyString());
  }

  @Test
  public void test_transfer_same_principal_is_bad_request() {
    assertThatThrownBy(() -> resource.transfer("alice", "alice", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(subjectAclService);
  }

  @Test
  public void test_transfer_missing_principal_is_bad_request() {
    assertThatThrownBy(() -> resource.transfer(null, "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> resource.transfer("alice", "", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(subjectAclService);
  }

  /**
   * The lookup is by type: a group with the same name does not count.
   */
  @Test
  public void test_transfer_from_without_acl_is_bad_request() {
    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verify(subjectAclService).findSubjectAcl(USER, "alice", RESOURCE, INSTANCE);
    verifyNoWrites();
  }

  /**
   * Rejected on the document lookup, before the file lookup (lenient stub).
   */
  @Test
  public void test_transfer_from_with_file_acl_only_is_bad_request() {
    lenient().when(subjectAclService.findSubjectAcl(USER, "alice", FILE_RESOURCE, FILE_INSTANCE))
      .thenReturn(Optional.of(acl("alice", FILE_RESOURCE, EDITOR)));

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();
  }

  @Test
  public void test_transfer_invalid_type_is_bad_request() {
    assertThatThrownBy(() -> resource.transfer("alice", "bob", "robot", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();
  }

  @Test
  public void test_transfer_on_published_resource_is_bad_request() {
    resource.setResourceInstance("/individual-study", INSTANCE);

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(subjectAclService);
  }

  @Test
  public void test_transfer_is_admin_only() throws NoSuchMethodException {
    RequiresRoles roles = SubjectAclResource.class
      .getMethod("transfer", String.class, String.class, String.class, String.class)
      .getAnnotation(RequiresRoles.class);

    assertThat(roles).isNotNull();
    assertThat(roles.value()).containsExactly(Roles.MICA_ADMIN);
  }

  @Test
  public void test_transfer_requires_edit_permission() {
    doThrow(new AuthorizationException()).when(subjectAclService).checkPermission(RESOURCE, "EDIT", INSTANCE);

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(AuthorizationException.class);
    verifyNoWrites();
  }

  private void verifyNoWrites() {
    verify(subjectAclService, never()).addSubjectPermission(any(), anyString(), anyString(), anyString(), anyString());
    verify(subjectAclService, never()).removeSubjectPermissions(any(), anyString(), anyString(), anyString());
  }

  private void given(String principal, String res, String inst, String actions) {
    when(subjectAclService.findSubjectAcl(USER, principal, res, inst)).thenReturn(Optional.of(acl(principal, res, actions)));
  }

  private static SubjectAcl acl(String principal, String res, String actions) {
    return SubjectAcl.newBuilder(principal, USER).resource(res).action(actions).instance(INSTANCE).build();
  }
}
