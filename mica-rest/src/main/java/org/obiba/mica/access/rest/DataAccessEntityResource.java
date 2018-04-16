package org.obiba.mica.access.rest;

import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequestStatus;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.service.SubjectAclService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public abstract class DataAccessEntityResource {

  @Inject
  protected SubjectAclService subjectAclService;

  protected abstract DataAccessEntityService getService();

  protected abstract String getId();

  abstract String getResourcePath();

  @PUT
  @Path("/_status")
  public Response updateStatus(@QueryParam("to") String status) {
    subjectAclService.checkPermission(getResourcePath() + "/" + getId(), "EDIT", "_status");

    switch(DataAccessRequestStatus.valueOf(status.toUpperCase())) {
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

  //
  // Private methods
  //

  protected Response submit() {
    String id  = getId();
    DataAccessEntity request = getService().findById(id);
    boolean fromOpened = request.getStatus() == DataAccessRequestStatus.OPENED;
    boolean fromConditionallyApproved = request.getStatus() == DataAccessRequestStatus.CONDITIONALLY_APPROVED;
    if(fromOpened && !subjectAclService.isCurrentUser(request.getApplicant())) {
      // only applicant can submit an opened request
      throw new ForbiddenException();
    }
    getService().updateStatus(id, DataAccessRequestStatus.SUBMITTED);
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
    DataAccessEntity request = getService().updateStatus(getId(), DataAccessRequestStatus.OPENED);
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
    boolean fromConditionallyApproved = request.getStatus() == DataAccessRequestStatus.CONDITIONALLY_APPROVED;
    if (fromConditionallyApproved) {
      // remove applicant permissions
      subjectAclService.removePermission(getResourcePath(), "EDIT,DELETE", id);
      subjectAclService.removePermission(getResourcePath()+ "/" + id, "EDIT", "_status");
      // data access officers can change the status of the request
      subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");
    }
    return updateStatus(DataAccessRequestStatus.REVIEWED);
  }

  protected Response approve() {
    return updateStatus(DataAccessRequestStatus.APPROVED);
  }

  protected Response reject() {
    return updateStatus(DataAccessRequestStatus.REJECTED);
  }

  protected Response conditionallyApprove() {
    String id = getId();
    DataAccessEntity request = getService().updateStatus(id, DataAccessRequestStatus.CONDITIONALLY_APPROVED);
    // restore applicant permissions
    subjectAclService.addUserPermission(request.getApplicant(), getResourcePath(), "VIEW,EDIT,DELETE", id);
    subjectAclService.addUserPermission(request.getApplicant(), getResourcePath()+ "/" + id, "EDIT", "_status");
    // data access officers cannot change the status of this request anymore
    subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");
    return updateStatus(DataAccessRequestStatus.CONDITIONALLY_APPROVED);
  }

  protected Response updateStatus(DataAccessRequestStatus status) {
    getService().updateStatus(getId(), status);
    return Response.noContent().build();
  }
}
