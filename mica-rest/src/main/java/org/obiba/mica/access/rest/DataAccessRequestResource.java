package org.obiba.mica.access.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;

@Component
@Path("/data-access-request/{id}")
public class DataAccessRequestResource {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

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

  @DELETE
  public Response delete(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request", "EDIT", id);
    try {
      dataAccessRequestService.delete(id);
      eventBus.post(new ResourceDeletedEvent("/data-access-request", id));
    } catch(NoSuchDataAccessRequestException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  @POST
  @Path("/comments")
  public Response comment(@PathParam("id") String id) {
    subjectAclService.checkPermission("/data-access-request/" + id + "/comments", "ADD");
    // TODO
    return Response.noContent().build();
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
  public Response approved(@PathParam("id") String id) {
    return updateStatus(id, DataAccessRequest.Status.APPROVED);
  }

  @PUT
  @Path("/_reject")
  public Response rejected(@PathParam("id") String id) {
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
