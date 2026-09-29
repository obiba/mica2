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

import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessAmendmentDeletedEvent;
import org.obiba.mica.access.event.DataAccessFeasibilityDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.event.CommentDeletedEvent;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.service.SchemaFormContentFileService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataAccessRequestServiceTest {

  @InjectMocks
  private DataAccessRequestService dataAccessRequestService;

  @Mock
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Mock
  private AttachmentRepository attachmentRepository;

  @Mock
  private FileStoreService fileStoreService;

  @Mock
  private SchemaFormContentFileService schemaFormContentFileService;

  @Mock
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Mock
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  @Mock
  private DataAccessPreliminaryService dataAccessPreliminaryService;

  @Mock
  private DataAccessAgreementService dataAccessAgreementService;

  @Mock
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  @Mock
  private EventBus eventBus;

  /**
   * The attachments are deleted by deleteWithReferences only: deleting them a second time failed on the version check
   * of the already deleted documents, before the deleted event was posted (so the request acls were never removed).
   */
  @Test
  public void test_delete_request_with_attachments_deletes_them_once_and_posts_deleted_event() {
    Attachment attachment = new Attachment();
    attachment.setId("a1");
    attachment.setFileReference("file-a1");
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    request.setId("111111");
    request.setAttachments(Lists.newArrayList(attachment));
    when(dataAccessRequestRepository.findById("111111")).thenReturn(Optional.of(request));

    dataAccessRequestService.delete("111111");

    InOrder order = inOrder(dataAccessRequestRepository, eventBus);
    order.verify(dataAccessRequestRepository).deleteWithReferences(request);
    ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
    order.verify(eventBus).post(event.capture());
    assertThat(event.getValue()).isInstanceOf(DataAccessRequestDeletedEvent.class);
    assertThat(((DataAccessRequestDeletedEvent) event.getValue()).getPersistable()).isSameAs(request);
    verify(dataAccessCollaboratorService).deleteAll("111111");
    verifyNoInteractions(attachmentRepository, fileStoreService);
  }

  /**
   * The agreements of the request (one per applicant/collaborator) are deleted with it, otherwise a new request
   * reusing the id would pick them up again (DataAccessAgreementService.getOrCreate looks them up by parent id).
   */
  @Test
  public void test_delete_request_deletes_its_agreements() {
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    request.setId("111111");
    when(dataAccessRequestRepository.findById("111111")).thenReturn(Optional.of(request));
    when(dataAccessAgreementService.findByParentId("111111"))
      .thenReturn(Lists.newArrayList(agreement("111111-applicant"), agreement("111111-collaborator")));

    dataAccessRequestService.delete("111111");

    verify(dataAccessAgreementService).delete("111111-applicant");
    verify(dataAccessAgreementService).delete("111111-collaborator");
    // the request acls, including the agreement ones, are removed by the deleted event listener, after the agreements
    InOrder order = inOrder(dataAccessAgreementService, eventBus);
    order.verify(dataAccessAgreementService).delete("111111-collaborator");
    order.verify(eventBus).post(any(DataAccessRequestDeletedEvent.class));
  }

  /**
   * When a request is deleted, its amendments, feasibilities and comments are deleted after it: their listeners must not
   * fail (and log an error) trying to touch the request that is already gone.
   */
  @Test
  public void test_children_deleted_after_their_request_do_not_touch_it() {
    when(dataAccessRequestRepository.findById("111111")).thenReturn(Optional.empty());

    dataAccessRequestService.dataAccessAmendmentDeleted(new DataAccessAmendmentDeletedEvent(amendment()));
    dataAccessRequestService.dataAccessFeasibilityDeleted(new DataAccessFeasibilityDeletedEvent(feasibility()));
    dataAccessRequestService.commentDeleted(new CommentDeletedEvent(comment()));

    verify(dataAccessRequestRepository, never()).saveWithReferences(any());
  }

  /**
   * A child or a comment deleted on its own still updates the last modification date of its request.
   */
  @Test
  public void test_children_deleted_alone_touch_their_request() {
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    request.setId("111111");
    when(dataAccessRequestRepository.findById("111111")).thenReturn(Optional.of(request));

    dataAccessRequestService.dataAccessAmendmentDeleted(new DataAccessAmendmentDeletedEvent(amendment()));
    dataAccessRequestService.dataAccessFeasibilityDeleted(new DataAccessFeasibilityDeletedEvent(feasibility()));
    dataAccessRequestService.commentDeleted(new CommentDeletedEvent(comment()));

    verify(dataAccessRequestRepository, times(3)).saveWithReferences(request);
    assertThat(request.getLastModifiedDate()).isPresent();
  }

  private static DataAccessAmendment amendment() {
    DataAccessAmendment amendment = (DataAccessAmendment) DataAccessAmendment.newBuilder().parentId("111111").build();
    amendment.setId("111111-A1");
    return amendment;
  }

  private static DataAccessFeasibility feasibility() {
    DataAccessFeasibility feasibility = (DataAccessFeasibility) DataAccessFeasibility.newBuilder().parentId("111111").build();
    feasibility.setId("111111-F1");
    return feasibility;
  }

  private static Comment comment() {
    return Comment.newBuilder().resourceId("/data-access-request").instanceId("111111").message("hello").build();
  }

  private static DataAccessAgreement agreement(String id) {
    DataAccessAgreement agreement = (DataAccessAgreement) DataAccessAgreement.newBuilder().parentId("111111").build();
    agreement.setId(id);
    return agreement;
  }
}
