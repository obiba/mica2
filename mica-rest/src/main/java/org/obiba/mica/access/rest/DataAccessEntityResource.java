package org.obiba.mica.access.rest;

import java.io.IOException;

import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.codahale.metrics.annotation.Timed;

public abstract class DataAccessEntityResource {

  @Inject
  protected SubjectAclService subjectAclService;

  @Inject
  protected FileStoreService fileStoreService;

  protected abstract DataAccessEntityService getService();

  protected abstract String getId();

  abstract String getResourcePath();

  @PUT
  @Path("/_status")
  public Response updateStatus(@QueryParam("to") String status) {
    subjectAclService.checkPermission(getResourcePath() + "/" + getId(), "EDIT", "_status");

    switch(DataAccessEntityStatus.valueOf(status.toUpperCase())) {
      case SUBMITTED:
        return submit();
      case OPENED:
        return open();
      case REVIEWED:
        return review();
      case CONDITIONALLY_APPROVED:
        return conditionallyApprove();
      case APPROVED:
        return approve();
      case REJECTED:
        return reject();
    }
    throw new BadRequestException("Unknown status");
  }

  @GET
  @Timed
  @Path("/form/attachments/{attachmentName}/{attachmentId}/_download")
  public Response getFormAttachment(@PathParam("attachmentName") String attachmentName,
    @PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", getId());
    getService().findById(getId());
    return Response.ok(fileStoreService.getFile(attachmentId)).header("Content-Disposition",
      "attachment; filename=\"" + attachmentName + "\"")
      .build();
  }

  //
  // Private methods
  //

  protected Response submit() {
    String id  = getId();
    DataAccessEntity request = getService().findById(id);
    boolean fromOpened = request.getStatus() == DataAccessEntityStatus.OPENED;
    boolean fromConditionallyApproved = request.getStatus() == DataAccessEntityStatus.CONDITIONALLY_APPROVED;
    if(fromOpened && !subjectAclService.isCurrentUser(request.getApplicant())) {
      // only applicant can submit an opened request
      throw new ForbiddenException();
    }
    getService().updateStatus(id, DataAccessEntityStatus.SUBMITTED);
    if (fromOpened || fromConditionallyApproved) {
      // applicant cannot edit, nor delete request anymore + status cannot be changed
      subjectAclService.removePermission(getResourcePath(), "EDIT,DELETE", id);
      subjectAclService.removePermission(getResourcePath()+ "/" + id, "EDIT", "_status");
      // data access officers can change the status of this request
      subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");
    }
    return Response.noContent().build();
  }

  protected Response open() {
    String id = getId();
    DataAccessEntity request = getService().updateStatus(getId(), DataAccessEntityStatus.OPENED);
    // restore applicant permissions
    subjectAclService.addUserPermission(request.getApplicant(), getResourcePath(), "VIEW,EDIT,DELETE", id);
    subjectAclService.addUserPermission(request.getApplicant(), getResourcePath()+ "/" + id, "EDIT", "_status");
    // data access officers cannot change the status of this request anymore
    subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");
    return Response.noContent().build();
  }

  protected Response review() {
    String id = getId();
    DataAccessEntity request = getService().findById(id);
    boolean fromConditionallyApproved = request.getStatus() == DataAccessEntityStatus.CONDITIONALLY_APPROVED;
    if (fromConditionallyApproved) {
      // remove applicant permissions
      subjectAclService.removePermission(getResourcePath(), "EDIT,DELETE", id);
      subjectAclService.removePermission(getResourcePath()+ "/" + id, "EDIT", "_status");
      // data access officers can change the status of the request
      subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");
    }
    return updateStatus(DataAccessEntityStatus.REVIEWED);
  }

  protected Response approve() {
    return updateStatus(DataAccessEntityStatus.APPROVED);
  }

  protected Response reject() {
    return updateStatus(DataAccessEntityStatus.REJECTED);
  }

  protected Response conditionallyApprove() {
    String id = getId();
    DataAccessEntity request = getService().updateStatus(id, DataAccessEntityStatus.CONDITIONALLY_APPROVED);
    // restore applicant permissions
    subjectAclService.addUserPermission(request.getApplicant(), getResourcePath(), "VIEW,EDIT,DELETE", id);
    subjectAclService.addUserPermission(request.getApplicant(), getResourcePath()+ "/" + id, "EDIT", "_status");
    // data access officers cannot change the status of this request anymore
    subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");
    return updateStatus(DataAccessEntityStatus.CONDITIONALLY_APPROVED);
  }

  protected Response updateStatus(DataAccessEntityStatus status) {
    getService().updateStatus(getId(), status);
    return Response.noContent().build();
  }
}
