/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.comment.rest;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.security.service.SubjectAclService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A comment author keeps its comment permissions when its access to the document is revoked: editing or deleting the
 * comment also requires access to the document.
 */
@ExtendWith(MockitoExtension.class)
public class CommentResourceTest {

  private static final String DOCUMENT = "/draft/individual-study";

  private static final String COMMENT = "/draft/individual-study/s1/comment";

  @InjectMocks
  private CommentResource resource;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private CommentsService commentsService;

  @Mock
  private AbstractGitPersistableService service;

  @BeforeEach
  public void setUp() {
    when(service.getTypeName()).thenReturn("individual-study");
    resource.setService(service);
  }

  @Test
  public void test_update_without_document_access_is_forbidden() {
    doThrow(new AuthorizationException()).when(subjectAclService).checkPermission(DOCUMENT, "VIEW", "s1");

    assertThatThrownBy(() -> resource.updateComment("s1", "c1", "edited")).isInstanceOf(AuthorizationException.class);
    verify(subjectAclService, never()).checkPermission(COMMENT, "EDIT", "c1");
    verifyNoInteractions(commentsService);
  }

  @Test
  public void test_delete_without_document_access_is_forbidden() {
    doThrow(new AuthorizationException()).when(subjectAclService).checkPermission(DOCUMENT, "VIEW", "s1");

    assertThatThrownBy(() -> resource.deleteComment("s1", "c1")).isInstanceOf(AuthorizationException.class);
    verify(subjectAclService, never()).checkPermission(COMMENT, "DELETE", "c1");
    verifyNoInteractions(commentsService);
  }

  @Test
  public void test_delete_with_document_access_still_requires_comment_permission() {
    // lenient: checkPermission is first called, unstubbed, for the document
    lenient().doThrow(new AuthorizationException()).when(subjectAclService).checkPermission(COMMENT, "DELETE", "c1");

    assertThatThrownBy(() -> resource.deleteComment("s1", "c1")).isInstanceOf(AuthorizationException.class);
    verify(subjectAclService).checkPermission(DOCUMENT, "VIEW", "s1");
    verify(commentsService, never()).delete(anyString());
  }
}
