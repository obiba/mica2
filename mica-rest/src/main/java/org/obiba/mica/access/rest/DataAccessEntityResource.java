package org.obiba.mica.access.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.Subscribe;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.event.DataAccessFormUpdatedEvent;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

public abstract class DataAccessEntityResource<T extends DataAccessEntity> {

  protected SubjectAclService subjectAclService;

  protected FileStoreService fileStoreService;

  private DataAccessFormService dataAccessFormService;

  protected abstract DataAccessEntityService<T> getService();

  protected abstract String getId();

  abstract String getResourcePath();

  public DataAccessEntityResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessFormService dataAccessFormService) {
    this.subjectAclService = subjectAclService;
    this.fileStoreService = fileStoreService;
    this.dataAccessFormService = dataAccessFormService;
  }

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

  @Subscribe
  public void onDataAccessFormUpdate(DataAccessFormUpdatedEvent event) {
    List<String> statuses = Stream.of(
      DataAccessEntityStatus.SUBMITTED,
      DataAccessEntityStatus.REVIEWED,
      DataAccessEntityStatus.APPROVED,
      DataAccessEntityStatus.REJECTED).map(DataAccessEntityStatus::name).collect(Collectors.toList());

    if (event.getForm().isDaoCanEdit()) {
      getService().findByStatus(statuses).forEach(darEntity -> subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", darEntity.getId()));
    } else {
      getService().findByStatus(statuses).forEach(darEntity -> subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", darEntity.getId()));
    }
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
      restoreDaoActions(id);
    }
    return Response.noContent().build();
  }

  protected Response open() {
    String id = getId();
    DataAccessEntity request = getService().updateStatus(getId(), DataAccessEntityStatus.OPENED);
    restoreApplicantActions(id, request.getApplicant());
    return Response.noContent().build();
  }

  protected Response review() {
    String id = getId();
    DataAccessEntity request = getService().findById(id);
    boolean fromConditionallyApproved = request.getStatus() == DataAccessEntityStatus.CONDITIONALLY_APPROVED;
    if (fromConditionallyApproved) {
      restoreDaoActions(id);
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
    restoreApplicantActions(id, request.getApplicant());
    return updateStatus(DataAccessEntityStatus.CONDITIONALLY_APPROVED);
  }

  protected Response updateStatus(DataAccessEntityStatus status) {
    getService().updateStatus(getId(), status);
    return Response.noContent().build();
  }

  private void restoreApplicantActions(String id, String applicant) {
    // restore applicant permissions, i.e applicant cannot edit, nor delete request anymore + status cannot be changed
    subjectAclService.addUserPermission(applicant, getResourcePath(), "VIEW,EDIT,DELETE", id);
    subjectAclService.addUserPermission(applicant, getResourcePath()+ "/" + id, "EDIT", "_status");
    // data access officers cannot change the status of this request anymore
    subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");

    if (dataAccessFormService.find().get().isDaoCanEdit()) {
      subjectAclService.removeGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", id);
    }
  }

  private void restoreDaoActions(String id) {
    // remove applicant permissions
    subjectAclService.removePermission(getResourcePath(), "EDIT,DELETE", id);
    subjectAclService.removePermission(getResourcePath()+ "/" + id, "EDIT", "_status");
    // data access officers can change the status of this request
    subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath()+ "/" + id, "EDIT", "_status");

    if (dataAccessFormService.find().get().isDaoCanEdit()) {
      subjectAclService.addGroupPermission(Roles.MICA_DAO, getResourcePath(), "EDIT", id);
    }
  }
}
