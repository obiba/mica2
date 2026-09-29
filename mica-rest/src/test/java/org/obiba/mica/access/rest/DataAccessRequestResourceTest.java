/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.notification.DataAccessRequestCommentMailNotification;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.core.service.SchemaFormContentFileService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.SchemaFormConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.springframework.context.ApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataAccessRequestResourceTest {

  @Mock
  private DataAccessRequestService dataAccessRequestService;

  @Mock
  private CommentsService commentsService;

  @Mock
  private SubjectAclService subjectAclService;

  private DataAccessRequestResource resource;

  @BeforeEach
  public void setUp() {
    resource = new DataAccessRequestResource(dataAccessRequestService,
      mock(DataAccessRequestCommentMailNotification.class), mock(DataAccessFormService.class), commentsService,
      mock(ApplicationContext.class), mock(EventBus.class), mock(Dtos.class), subjectAclService,
      mock(FileStoreService.class), mock(DataAccessConfigService.class), mock(TempFileService.class),
      mock(VariableSetService.class), mock(DataAccessRequestUtilService.class), mock(SchemaFormConfigService.class),
      mock(SchemaFormContentFileService.class));

    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("applicant");
    ThreadContext.bind(subject);
  }

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  /**
   * Deleting a request must delete its comments with the same resource id the comments are created and listed with
   * (it used the class name "DataAccessRequest", which never matched, leaving the comments behind).
   */
  @Test
  public void test_delete_removes_comments_with_the_resource_id_they_are_created_with() {
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    request.setId("111111");
    when(dataAccessRequestService.findById("111111")).thenReturn(request);
    when(commentsService.save(any(), any())).thenAnswer(invocation -> {
      Comment comment = invocation.getArgument(0);
      comment.setId("c1");
      return comment;
    });

    resource.createComment("111111", "hello", false);
    ArgumentCaptor<Comment> created = ArgumentCaptor.forClass(Comment.class);
    verify(commentsService).save(created.capture(), any());
    String resourceId = created.getValue().getResourceId();

    // the key used to list the comments of the request
    resource.comments("111111", false);
    verify(commentsService).findPublicComments(resourceId, "111111");

    resource.delete("111111");
    verify(dataAccessRequestService).delete("111111");
    verify(commentsService).delete(resourceId, "111111");
  }
}
