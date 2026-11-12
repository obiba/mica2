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

import java.util.Collections;
import java.util.List;

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

  private static final String EDITOR = PermissionsUtils.asActions("EDITOR");

  private static final String REVIEWER = PermissionsUtils.asActions("REVIEWER");

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
    lenient().when(subjectAclService.findByResourceInstance(anyString(), anyString())).thenReturn(Collections.emptyList());
    lenient().when(userProfileService.userHas(anyString(), eq(Roles.MICA_EXTERNAL_EDITOR))).thenReturn(true);
  }

  @Test
  public void test_transfer_to_non_external_editor_is_bad_request() {
    when(subjectAclService.findByResourceInstance(RESOURCE, INSTANCE)).thenReturn(List.of(acl("alice", RESOURCE, EDITOR)));
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
  public void test_transfer_moves_instance_and_file_acls_keeping_role() {
    when(subjectAclService.findByResourceInstance(RESOURCE, INSTANCE)).thenReturn(List.of(acl("alice", RESOURCE, EDITOR)));
    when(subjectAclService.findByResourceInstance(FILE_RESOURCE, FILE_INSTANCE)).thenReturn(List.of(acl("alice", FILE_RESOURCE, EDITOR)));

    Response response = resource.transfer("alice", "bob", "USER", null);

    assertThat(response.getStatus()).isEqualTo(204);
    InOrder order = inOrder(subjectAclService);
    order.verify(subjectAclService).checkPermission(RESOURCE, "EDIT", INSTANCE);
    order.verify(subjectAclService).addSubjectPermission(SubjectAcl.Type.USER, "bob", RESOURCE, EDITOR, INSTANCE);
    order.verify(subjectAclService).addSubjectPermission(SubjectAcl.Type.USER, "bob", FILE_RESOURCE, EDITOR, FILE_INSTANCE);
    order.verify(subjectAclService).removeSubjectPermissions(SubjectAcl.Type.USER, "alice", RESOURCE, INSTANCE);
    order.verify(subjectAclService).removeSubjectPermissions(SubjectAcl.Type.USER, "alice", FILE_RESOURCE, FILE_INSTANCE);
  }

  @Test
  public void test_transfer_with_role_and_no_file_acl() {
    when(subjectAclService.findByResourceInstance(RESOURCE, INSTANCE)).thenReturn(List.of(acl("alice", RESOURCE, EDITOR)));

    resource.transfer("alice", "bob", "USER", "reviewer");

    verify(subjectAclService).addSubjectPermission(SubjectAcl.Type.USER, "bob", RESOURCE, REVIEWER, INSTANCE);
    verify(subjectAclService, never()).addSubjectPermission(any(), anyString(), eq(FILE_RESOURCE), anyString(), anyString());
    verify(subjectAclService).removeSubjectPermissions(SubjectAcl.Type.USER, "alice", RESOURCE, INSTANCE);
    verify(subjectAclService, never()).removeSubjectPermissions(SubjectAcl.Type.USER, "alice", FILE_RESOURCE, FILE_INSTANCE);
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

  @Test
  public void test_transfer_from_without_acl_is_bad_request() {
    // a group with the same name does not count
    when(subjectAclService.findByResourceInstance(RESOURCE, INSTANCE))
      .thenReturn(List.of(SubjectAcl.newBuilder("alice", SubjectAcl.Type.GROUP).resource(RESOURCE).action(EDITOR).instance(INSTANCE).build()));

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", null)).isInstanceOf(BadRequestException.class);
    verifyNoWrites();
  }

  @Test
  public void test_transfer_invalid_role_or_type_is_bad_request() {
    lenient().when(subjectAclService.findByResourceInstance(RESOURCE, INSTANCE)).thenReturn(List.of(acl("alice", RESOURCE, EDITOR)));

    assertThatThrownBy(() -> resource.transfer("alice", "bob", "USER", "owner")).isInstanceOf(BadRequestException.class);
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

  private static SubjectAcl acl(String principal, String res, String actions) {
    return SubjectAcl.newBuilder(principal, SubjectAcl.Type.USER).resource(res).action(actions).instance(INSTANCE).build();
  }
}
