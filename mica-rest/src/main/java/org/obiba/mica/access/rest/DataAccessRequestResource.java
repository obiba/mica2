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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.notification.DataAccessRequestCommentMailNotification;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.Mica.DataAccessRequestDto.StatusChangeDto;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import sun.util.locale.LanguageTag;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/data-access-request/{id}")
public class DataAccessRequestResource extends DataAccessEntityResource {
  private static final Logger log = getLogger(DataAccessRequestResource.class);

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessRequestCommentMailNotification commentMailNotification;

  @Inject
  private CommentsService commentsService;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private Dtos dtos;

  @Inject
  private EventBus eventBus;

  @PathParam("id")
  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto get() {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    return dtos.asDto(request);
  }

  @GET
  @Path("/_history")
  public List<StatusChangeDto> getLoggedHistory() {
    Map<String, List<StatusChange>> mergedStatusChangHistory = dataAccessRequestService.getMergedStatusChangHistory(id);

    return mergedStatusChangHistory.entrySet().stream().map(entry -> entry.getValue().stream().map(
      statusChange -> dtos.asDto(statusChange).toBuilder()
        .setReference(!entry.getKey().equals(DataAccessRequestService.DAR_ROOT_KEY) ? entry.getKey() : "").build())
      .collect(Collectors.toList()))
      .flatMap(List::stream)
      .sorted(Comparator.comparing(StatusChangeDto::getChangedOn))
      .collect(Collectors.toList());
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    return JSONUtils.toMap(dataAccessRequestService.findById(id).getContent());
  }

  @PUT
  @Path("/_logActions")
  @Timed
  public Response updateActionLogs(Mica.DataAccessRequestDto dto) {
    SecurityUtils.getSubject().checkRoles(Roles.MICA_DAO, Roles.MICA_ADMIN);
    if(!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest request = dtos.fromDto(dto);
    dataAccessRequestService.save(request);
    return Response.noContent().build();
  }

  @PUT
  @Timed
  public Response put(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    if(!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest request = dtos.fromDto(dto);
    dataAccessRequestService.save(request);
    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/_pdf")
  public Response getPdf(@QueryParam("lang") String lang) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);

    if(Strings.isNullOrEmpty(lang)) lang = LanguageTag.UNDETERMINED;

    return Response.ok(dataAccessRequestService.getRequestPdf(id, lang))
      .header("Content-Disposition", "attachment; filename=\"" + "data-access-request-" + id + ".pdf" + "\"").build();
  }

  @PUT
  @Timed
  @Path("/_attachments")
  public Response updateAttachments(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    if(!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest request = dtos.fromDto(dto);
    dataAccessRequestService.saveAttachments(request);
    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/attachments/{attachmentId}/_download")
  public Response getAttachment(@PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    Optional<Attachment> r = request.getAttachments().stream().filter(a -> a.getId().equals(attachmentId)).findFirst();

    if(!r.isPresent()) throw NoSuchEntityException.withId(Attachment.class, attachmentId);

    return Response.ok(fileStoreService.getFile(r.get().getFileReference()))
      .header("Content-Disposition", "attachment; filename=\"" + r.get().getName() + "\"").build();
  }

  @GET
  @Timed
  @Path("/form/attachments/{attachmentName}/{attachmentId}/_download")
  public Response getFormAttachment(@PathParam("attachmentName") String attachmentName,
    @PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    dataAccessRequestService.findById(id);
    return Response.ok(fileStoreService.getFile(attachmentId))
      .header("Content-Disposition", "attachment; filename=\"" + attachmentName + "\"").build();
  }

  @DELETE
  public Response delete(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "DELETE", id);
    try {
      dataAccessRequestService.delete(id);
      // remove associated comments
      commentsService.delete(DataAccessRequest.class.getSimpleName(), id);
    } catch(NoSuchDataAccessRequestException e) {
      log.error("Could not delete data-access-request {}", e);
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/comments")
  public List<Mica.CommentDto> comments() {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    dataAccessRequestService.findById(id);
    return commentsService.findByResourceAndInstance("/data-access-request", id).stream().map(dtos::asDto)
      .collect(Collectors.toList());
  }

  @POST
  @Path("/comments")
  public Response createComment(String message) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    dataAccessRequestService.findById(id);
    Comment comment = commentsService.save( //
      Comment.newBuilder() //
        .createdBy(SecurityUtils.getSubject().getPrincipal().toString()) //
        .message(message) //
        .resourceId("/data-access-request") //
        .instanceId(id) //
        .build(), commentMailNotification); //

    subjectAclService.addPermission("/data-access-request/" + id + "/comment", "VIEW,EDIT,DELETE", comment.getId());
    subjectAclService
      .addGroupPermission(Roles.MICA_DAO, "/data-access-request/" + id + "/comment", "DELETE", comment.getId());

    return Response.noContent().build();
  }

  @GET
  @Path("/comment/{commentId}")
  public Mica.CommentDto getComment(@PathParam("commentId") String commentId) {
    subjectAclService.checkPermission("/data-access-request/", "VIEW", id);
    dataAccessRequestService.findById(id);
    return dtos.asDto(commentsService.findById(commentId));
  }

  @PUT
  @Path("/comment/{commentId}")
  public Response updateComment(@PathParam("commentId") String commentId, String message) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comment", "EDIT", commentId);
    dataAccessRequestService.findById(id);

    commentsService.save(Comment.newBuilder(commentsService.findById(commentId)) //
      .message(message) //
      .modifiedBy(SecurityUtils.getSubject().getPrincipal().toString()) //
      .build(), commentMailNotification); //

    return Response.noContent().build();
  }

  @DELETE
  @Path("/comment/{commentId}")
  public Response deleteComment(@PathParam("commentId") String commentId) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comment", "DELETE", commentId);
    dataAccessRequestService.findById(id);
    commentsService.delete(commentId);
    eventBus.post(new ResourceDeletedEvent("/data-access-request/" + id + "/comment", commentId));
    return Response.noContent().build();
  }

  @Path("/amendments")
  public DataAccessAmendmentsResource getAmendments() {
    dataAccessRequestService.findById(id);
    DataAccessAmendmentsResource dataAccessAmendmentsResource = applicationContext
      .getBean(DataAccessAmendmentsResource.class);
    dataAccessAmendmentsResource.setParentId(id);
    return dataAccessAmendmentsResource;
  }

  @Path("/amendment/{amendmentId}")
  public DataAccessAmendmentResource getAmendment(@PathParam("amendmentId") String amendmentId) {
    dataAccessRequestService.findById(id);
    DataAccessAmendmentResource dataAccessAmendmentResource = applicationContext
      .getBean(DataAccessAmendmentResource.class);
    dataAccessAmendmentResource.setParentId(id);
    dataAccessAmendmentResource.setId(amendmentId);
    return dataAccessAmendmentResource;
  }

  @Override
  protected DataAccessEntityService getService() {
    return dataAccessRequestService;
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  String getResourcePath() {
    return "/data-access-request";
  }

  @Override
  protected Response updateStatus(DataAccessEntityStatus status) {
    DataAccessRequest request = dataAccessRequestService.findById(id);
    String resource = String.format("/data-access-request/%s/amendment", id);
    String applicant = request.getApplicant();

    if(DataAccessEntityStatus.APPROVED.equals(status)) {
      subjectAclService.addUserPermission(applicant, resource, "ADD", null);
      subjectAclService.addGroupPermission(Roles.MICA_DAO, resource, "VIEW,DELETE", null);
    } else {
      subjectAclService.removeUserPermission(applicant, resource, "ADD", null);
    }

    return super.updateStatus(status);
  }
}
