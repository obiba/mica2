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
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.joda.time.DateTime;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequestTimeline;
import org.obiba.mica.access.notification.DataAccessRequestCommentMailNotification;
import org.obiba.mica.access.notification.DataAccessRequestReportNotificationService;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.domain.NoSuchCommentException;
import org.obiba.mica.core.domain.UnauthorizedCommentException;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.TempFile;
import org.obiba.mica.file.service.TempFileService;
import org.obiba.mica.micaConfig.DataAccessAmendmentsNotEnabled;
import org.obiba.mica.micaConfig.DataAccessFeasibilityNotEnabled;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import sun.util.locale.LanguageTag;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;


@Component
@Path("/data-access-request/{id}")
@RequiresAuthentication
public class DataAccessRequestResource extends DataAccessEntityResource<DataAccessRequest> {

  private static final Logger log = getLogger(DataAccessRequestResource.class);

  private DataAccessRequestService dataAccessRequestService;

  private DataAccessRequestCommentMailNotification commentMailNotification;

  private DataAccessRequestReportNotificationService reportNotificationService;

  private CommentsService commentsService;

  private TempFileService tempFileService;

  private ApplicationContext applicationContext;

  private Dtos dtos;

  private EventBus eventBus;


  @Inject
  public DataAccessRequestResource(
    DataAccessRequestService dataAccessRequestService,
    DataAccessRequestCommentMailNotification commentMailNotification,
    DataAccessRequestReportNotificationService reportNotificationService,
    CommentsService commentsService,
    ApplicationContext applicationContext,
    EventBus eventBus,
    Dtos dtos,
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessFormService dataAccessFormService,
    TempFileService tempFileService,
    VariableSetService variableSetService) {
    super(subjectAclService, fileStoreService, dataAccessFormService, variableSetService);
    this.dataAccessRequestService = dataAccessRequestService;
    this.commentMailNotification = commentMailNotification;
    this.reportNotificationService = reportNotificationService;
    this.commentsService = commentsService;
    this.tempFileService = tempFileService;
    this.applicationContext = applicationContext;
    this.eventBus = eventBus;
    this.dtos = dtos;
  }

  @GET
  @Timed
  public Mica.DataAccessRequestDto get(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    return dtos.asDto(request);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    return JSONUtils.toMap(dataAccessRequestService.findById(id).getContent());
  }

  @PUT
  @Path("/model")
  @Consumes("application/json")
  public Response setModel(@PathParam("id") String id, String content) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");
    request.setContent(content);
    dataAccessRequestService.save(request);
    return Response.ok().build();
  }

  @PUT
  @Timed
  public Response put(@PathParam("id") String id, Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    if (!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest originalRequest = dataAccessRequestService.findById(id);
    if (originalRequest.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessRequest request = dtos.fromDto(dto);
    dataAccessRequestService.save(request);
    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/_pdf")
  public Response getPdf(@PathParam("id") String id, @QueryParam("lang") String lang) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);

    if (Strings.isNullOrEmpty(lang)) lang = LanguageTag.UNDETERMINED;

    return Response.ok(dataAccessRequestService.getRequestPdf(id, lang))
      .header("Content-Disposition", "attachment; filename=\"" + "data-access-request-" + id + ".pdf" + "\"").build();
  }

  @PUT
  @Path("/_applicant")
  public Response changeApplicant(@PathParam("id") String id, @QueryParam("username") String applicant) {
    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      throw new AuthorizationException();
    }
    if (Strings.isNullOrEmpty(applicant))
      throw new IllegalArgumentException("An applicant name is required");

    DataAccessRequest request = dataAccessRequestService.findById(id);
    String originalApplicant = request.getApplicant();
    dataAccessRequestService.changeApplicantAndSave(request, applicant);

    commentsService.findPublicComments("/data-access-request", id).stream()
      .filter(comment -> comment.getCreatedBy().equals(originalApplicant))
      .map(comment -> Comment.newBuilder(comment).createdBy(applicant).build())
      .forEach(comment -> commentsService.save(comment, null));

    return Response.noContent().build();
  }

  @PUT
  @Path("/_archive")
  public Response archive(@PathParam("id") String id) {
    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      throw new AuthorizationException();
    }
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (DataAccessEntityStatus.APPROVED.equals(request.getStatus())) {
      DataAccessRequestTimeline timeline = reportNotificationService.getReportsTimeline(request);
      if (!timeline.hasEndDate())
        throw new BadRequestException("Cannot archive: no data access request end date is defined");
      if (new Date().before(timeline.getEndDate()))
        throw new BadRequestException("Cannot archive: data access request end date not reached");
      request.setArchived(true);
      dataAccessRequestService.save(request);
    } else {
      throw new BadRequestException("Cannot archive: data access request must have been approved before being archived");
    }
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_archive")
  public Response unarchive(@PathParam("id") String id) {
    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      throw new AuthorizationException();
    }
    DataAccessRequest request = dataAccessRequestService.findById(id);
    request.setArchived(false);
    dataAccessRequestService.save(request);
    return Response.noContent().build();
  }

  @PUT
  @Path("/variables")
  public Response setVariablesSet(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    request.setVariablesSet(createOrUpdateVariablesSet(request));
    dataAccessRequestService.save(request);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/variables")
  public Response deleteVariablesSet(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.hasVariablesSet())
      variableSetService.delete(request.getVariablesSet());
    return Response.noContent().build();
  }

  @PUT
  @Path("/_start-date")
  @Timed
  public Response updateStartDate(@PathParam("id") String id, @QueryParam("date") String date) {
    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      throw new AuthorizationException();
    }
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");
    if (Strings.isNullOrEmpty(date))
      request.setStartDate(null);
    else {
      try {
        request.setStartDate(DataAccessRequestUtilService.ISO_8601.parse(date));
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    dataAccessRequestService.save(request);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_log-actions")
  @Timed
  public Response updateActionLogs(@PathParam("id") String id, Mica.DataAccessRequestDto dto) {
    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      throw new AuthorizationException();
    }
    if (!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest originalRequest = dataAccessRequestService.findById(id);
    if (originalRequest.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessRequest request = dtos.fromDto(dto);
    dataAccessRequestService.saveActionsLogs(request);
    return Response.noContent().build();
  }

  @POST
  @Path("/_log-actions")
  @Consumes("application/json")
  public Response addActionLog(@PathParam("id") String id, Map<String, String> action) {
    if (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN)) {
      throw new AuthorizationException();
    }
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");
    if (Strings.isNullOrEmpty(action.get("text"))) return Response.status(Response.Status.BAD_REQUEST).build();
    try {
      request.getActionLogHistory().add(ActionLog.newBuilder().action(action.get("text"))
        .changedOn(DateTime.parse(action.get("date")))
        .author(SecurityUtils.getSubject().getPrincipal().toString()).build());
      dataAccessRequestService.saveActionsLogs(request);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    return Response.noContent().build();
  }

  @PUT
  @Timed
  @Path("/_attachments")
  public Response updateAttachments(@PathParam("id") String id, Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    if (!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest originalRequest = dataAccessRequestService.findById(id);
    if (originalRequest.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessRequest request = dtos.fromDto(dto);
    dataAccessRequestService.saveAttachments(request);
    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/attachments/{attachmentId}/_download")
  public Response getAttachment(@PathParam("id") String id, @PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    Optional<Attachment> r = request.getAttachments().stream().filter(a -> a.getId().equals(attachmentId)).findFirst();

    if (!r.isPresent()) throw NoSuchEntityException.withId(Attachment.class, attachmentId);

    return Response.ok(fileStoreService.getFile(r.get().getFileReference()))
      .header("Content-Disposition", "attachment; filename=\"" + r.get().getName() + "\"").build();
  }

  @POST
  @Timed
  @Path("/attachments/{attachmentId}")
  public Response addAttachment(@PathParam("id") String id, @PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    TempFile tempFile = tempFileService.getMetadata(attachmentId);

    Attachment attachment = new Attachment();
    attachment.setId(tempFile.getId());
    attachment.setName(tempFile.getName());
    attachment.setSize(tempFile.getSize());
    attachment.setCreatedBy(tempFile.getCreatedBy());
    attachment.setCreatedDate(tempFile.getCreatedDate());
    attachment.setJustUploaded(true);

    request.getAttachments().add(attachment);
    dataAccessRequestService.saveAttachments(request);
    return Response.noContent().build();
  }

  @DELETE
  @Timed
  @Path("/attachments/{attachmentId}")
  public Response deleteAttachment(@PathParam("id") String id, @PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    request.setAttachments(request.getAttachments().stream().filter(a -> !a.getId().equals(attachmentId)).collect(Collectors.toList()));
    dataAccessRequestService.saveAttachments(request);
    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/form/attachments/{attachmentName}/{attachmentId}/_download")
  public Response getFormAttachment(@PathParam("id") String id, @PathParam("attachmentName") String attachmentName,
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
    } catch (NoSuchDataAccessRequestException e) {
      log.error("Could not delete data-access-request {}", e);
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/comments")
  public List<Mica.CommentDto> comments(@PathParam("id") String id, @QueryParam("admin") @DefaultValue("false") boolean admin) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    dataAccessRequestService.findById(id);
    if (admin) {
      if (!subjectAclService.isPermitted("/data-access-request/private-comment", "VIEW"))
        subjectAclService.checkPermission("/private-comment/data-access-request", "VIEW");
      return dtos.asDtos(commentsService.findPrivateComments("/data-access-request", id));
    } else {
      return dtos.asDtos(commentsService.findPublicComments("/data-access-request", id));
    }
  }

  @POST
  @Path("/comments")
  public Response createComment(@PathParam("id") String id, String message, @QueryParam("admin") @DefaultValue("false") boolean admin) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    Comment.Builder buildComment = Comment.newBuilder() //
      .createdBy(SecurityUtils.getSubject().getPrincipal().toString()) //
      .message(message) //
      .resourceId("/data-access-request") //
      .instanceId(id);

    if (admin) {
      if (!subjectAclService.isPermitted("/data-access-request/private-comment", "VIEW"))
        subjectAclService.checkPermission("/private-comment/data-access-request", "ADD");
      buildComment.admin(admin);
    }

    Comment comment = commentsService.save(buildComment.build(), commentMailNotification); //

    subjectAclService.addPermission("/data-access-request/" + id + "/comment", "VIEW,EDIT,DELETE", comment.getId());
    subjectAclService
      .addGroupPermission(Roles.MICA_DAO, "/data-access-request/" + id + "/comment", "DELETE", comment.getId());

    return Response.noContent().build();
  }

  @GET
  @Path("/comment/{commentId}")
  public Mica.CommentDto getComment(@PathParam("id") String id, @PathParam("commentId") String commentId) {
    subjectAclService.checkPermission("/data-access-request/", "VIEW", id);
    dataAccessRequestService.findById(id);

    List<Comment> commentAndNext = commentsService.findCommentAndNext(commentId, "/data-access-request/", id);
    if (commentAndNext.size() > 0) {
      return dtos.asDto(commentAndNext.get(0), commentAndNext.size() == 1);
    } else {
      throw new NoSuchCommentException(commentId);
    }
  }

  @PUT
  @Path("/comment/{commentId}")
  public Response updateComment(@PathParam("id") String id, @PathParam("commentId") String commentId, String message) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comment", "EDIT", commentId);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    commentsService.save(Comment.newBuilder(getCommentIfNotRepliedTo(id, commentId)) //
      .message(message) //
      .modifiedBy(SecurityUtils.getSubject().getPrincipal().toString()) //
      .build(), commentMailNotification); //

    return Response.noContent().build();
  }

  @DELETE
  @Path("/comment/{commentId}")
  public Response deleteComment(@PathParam("id") String id, @PathParam("commentId") String commentId) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comment", "DELETE", commentId);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    commentsService.delete(getCommentIfNotRepliedTo(id, commentId));
    eventBus.post(new ResourceDeletedEvent("/data-access-request/" + id + "/comment", commentId));

    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  public Response updateStatus(@PathParam("id") String id, @QueryParam("to") String status) {
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");
    return super.doUpdateStatus(id, status);
  }

  @Path("/feasibilities")
  public DataAccessFeasibilitiesResource getFeasibilities(@PathParam("id") String id) {
    if (!dataAccessRequestService.isFeasibilityEnabled()) throw new DataAccessFeasibilityNotEnabled();
    dataAccessRequestService.findById(id);
    DataAccessFeasibilitiesResource dataAccessFeasibilitiesResource = applicationContext
      .getBean(DataAccessFeasibilitiesResource.class);
    dataAccessFeasibilitiesResource.setParentId(id);
    return dataAccessFeasibilitiesResource;
  }

  @Path("/feasibility/{feasibilityId}")
  public DataAccessFeasibilityResource getFeasibility(@PathParam("id") String id, @PathParam("feasibilityId") String feasibilityId) {
    if (!dataAccessRequestService.isFeasibilityEnabled()) throw new DataAccessFeasibilityNotEnabled();
    dataAccessRequestService.findById(id);
    DataAccessFeasibilityResource dataAccessFeasibilityResource = applicationContext
      .getBean(DataAccessFeasibilityResource.class);
    dataAccessFeasibilityResource.setParentId(id);
    dataAccessFeasibilityResource.setId(feasibilityId);
    return dataAccessFeasibilityResource;
  }

  @Path("/amendments")
  public DataAccessAmendmentsResource getAmendments(@PathParam("id") String id) {
    if (!dataAccessRequestService.isAmendmentsEnabled()) throw new DataAccessAmendmentsNotEnabled();
    dataAccessRequestService.findById(id);
    DataAccessAmendmentsResource dataAccessAmendmentsResource = applicationContext
      .getBean(DataAccessAmendmentsResource.class);
    dataAccessAmendmentsResource.setParentId(id);
    return dataAccessAmendmentsResource;
  }

  @Path("/amendment/{amendmentId}")
  public DataAccessAmendmentResource getAmendment(@PathParam("id") String id, @PathParam("amendmentId") String amendmentId) {
    if (!dataAccessRequestService.isAmendmentsEnabled()) throw new DataAccessAmendmentsNotEnabled();
    dataAccessRequestService.findById(id);
    DataAccessAmendmentResource dataAccessAmendmentResource = applicationContext
      .getBean(DataAccessAmendmentResource.class);
    dataAccessAmendmentResource.setParentId(id);
    dataAccessAmendmentResource.setId(amendmentId);
    return dataAccessAmendmentResource;
  }

  @Override
  protected DataAccessEntityService<DataAccessRequest> getService() {
    return dataAccessRequestService;
  }

  @Override
  String getResourcePath() {
    return "/data-access-request";
  }

  @Override
  protected Response updateStatus(String id, DataAccessEntityStatus status) {
    DataAccessRequest request = dataAccessRequestService.findById(id);
    String resource = String.format("/data-access-request/%s/amendment", id);
    String applicant = request.getApplicant();

    if (DataAccessEntityStatus.APPROVED.equals(status)) {
      subjectAclService.addUserPermission(applicant, resource, "ADD", null);
      subjectAclService.addGroupPermission(Roles.MICA_DAO, resource, "VIEW,DELETE", null);
    } else {
      subjectAclService.removeUserPermission(applicant, resource, "ADD", null);
    }

    return super.updateStatus(id, status);
  }

  private Comment getCommentIfNotRepliedTo(String id, String commentId) {
    List<Comment> commentAndNext = commentsService.findCommentAndNext(commentId, "/data-access-request", id);

    if (commentAndNext.size() > 1) {
      throw new UnauthorizedCommentException(commentId);
    } else if (commentAndNext.size() == 0) {
      throw new NoSuchCommentException(commentId);
    }

    return commentAndNext.get(0);
  }
}
