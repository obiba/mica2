package org.obiba.mica.access.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

import sun.util.locale.LanguageTag;

@Component
@Path("/data-access-request/{id}")
public class DataAccessRequestResource {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private CommentsService commentsService;

  @Inject
  private Dtos dtos;

  @Inject
  private EventBus eventBus;

  @Inject
  private SubjectAclService subjectAclService;

  @GET
  @Timed
  public Mica.DataAccessRequestDto get(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "VIEW", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    return dtos.asDto(request);
  }

  @PUT
  @Timed
  public Response get(@PathParam("id") String id, Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    DataAccessRequest request = dtos.fromDto(dto);
    if (!id.equals(request.getId())) throw new BadRequestException();
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

  @DELETE
  public Response delete(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    try {
      dataAccessRequestService.delete(id);
      // remove associated comments
      commentsService.delete(DataAccessRequest.class.getSimpleName(), id);
      eventBus.post(new ResourceDeletedEvent("/data-access-request", id));
    } catch(NoSuchDataAccessRequestException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/comments")
  public List<Mica.CommentDto> comments(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request/", "VIEW", id);
    dataAccessRequestService.findById(id);
    return commentsService.findByClassAndInstance(DataAccessRequest.class.getSimpleName(), id).stream()
      .map(dtos::asDto).collect(Collectors.toList());
  }

  @POST
  @Path("/comments")
  public Response createComment(@PathParam("id") String id, String message) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comments", "ADD");
    dataAccessRequestService.findById(id);

    Comment comment = commentsService.save( //
      Comment.newBuilder() //
        .createdBy(SecurityUtils.getSubject().getPrincipal().toString()) //
        .message(message) //
        .className(DataAccessRequest.class.getSimpleName()) //
        .instanceId(id) //
        .build()); //

    subjectAclService.addPermission("/data-access-request/" + id + "/comment", "VIEW,EDIT", comment.getId());

    return Response.noContent().build();
  }

  @GET
  @Path("/comment/{commentId}")
  public Mica.CommentDto getComment(@PathParam("id") String id, @PathParam("commentId") String commentId) {
    subjectAclService.checkPermission("/data-access-request/", "VIEW", id);
    dataAccessRequestService.findById(id);
    return dtos.asDto(commentsService.findById(commentId));
  }

  @PUT
  @Path("/comment/{commentId}")
  public Response updateComment(@PathParam("id") String id, @PathParam("commentId") String commentId, String message) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comment", "EDIT", commentId);
    dataAccessRequestService.findById(id);

    commentsService.save(Comment.newBuilder(commentsService.findById(commentId)) //
      .message(message) //
      .modifiedBy(SecurityUtils.getSubject().getPrincipal().toString()) //
      .build()); //

    return Response.noContent().build();
  }

  @DELETE
  @Path("/comment/{commentId}")
  public Response deleteComment(@PathParam("id") String id, @PathParam("commentId") String commentId) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comment", "EDIT", commentId);
    dataAccessRequestService.findById(id);
    commentsService.delete(commentId);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  public Response updateStatus(@PathParam("id") String id, @QueryParam("to") String status) {
    switch (DataAccessRequest.Status.valueOf(status.toUpperCase())) {
      case SUBMITTED:
        return submit(id);
      case OPENED:
        return open(id);
      case REVIEWED:
        return review(id);
      case APPROVED:
        return approve(id);
      case REJECTED:
        return reject(id);
    }
    throw new BadRequestException("Invalid status");
  }

    @PUT
  @Path("/_submit")
  public Response submit(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(id);
    if(!subjectAclService.isCurrentUser(request.getApplicant())) {
      // only applicant can submit the request
      throw new ForbiddenException();
    }
    dataAccessRequestService.updateStatus(id, DataAccessRequest.Status.SUBMITTED);
    // applicant cannot edit request anymore
    subjectAclService.removePermission("/data-access-request", "EDIT", id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_reopen")
  public Response open(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.updateStatus(id, DataAccessRequest.Status.OPENED);
    subjectAclService.addUserPermission(request.getApplicant(), "/data-access-request", "EDIT", id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_review")
  public Response review(@PathParam("id") String id) {
    return updateStatus(id, DataAccessRequest.Status.REVIEWED);
  }

  @PUT
  @Path("/_approve")
  public Response approve(@PathParam("id") String id) {
    return updateStatus(id, DataAccessRequest.Status.APPROVED);
  }

  @PUT
  @Path("/_reject")
  public Response reject(@PathParam("id") String id) {
    return updateStatus(id, DataAccessRequest.Status.REJECTED);
  }

  //
  // Private methods
  //

  private Response updateStatus(String id, DataAccessRequest.Status status) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    dataAccessRequestService.updateStatus(id, status);
    return Response.noContent().build();
  }
}
