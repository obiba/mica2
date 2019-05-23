package org.obiba.mica.access.rest;


import com.codahale.metrics.annotation.Timed;
import java.io.IOException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessAmendmentResource extends DataAccessEntityResource<DataAccessAmendment> {

  private static final Logger log = getLogger(DataAccessAmendmentResource.class);

  private Dtos dtos;

  private DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  public DataAccessAmendmentResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessFormService dataAccessFormService,
    Dtos dtos,
    DataAccessAmendmentService dataAccessAmendmentService) {
    super(subjectAclService, fileStoreService, dataAccessFormService);
    this.dtos = dtos;
    this.dataAccessAmendmentService = dataAccessAmendmentService;
  }

  private String parentId;

  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto getAmendment() {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", parentId);
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    return dtos.asAmendentDto(amendment);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    return JSONUtils.toMap(dataAccessAmendmentService.findById(id).getContent());
  }

  @PUT
  @Timed
  public Response update(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    if(!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessAmendment request = dtos.fromAmendmentDto(dto);
    dataAccessAmendmentService.save(request);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    String resource = getResourcePath();
    subjectAclService.checkPermission(resource, "DELETE", id);

    try {
      dataAccessAmendmentService.delete(id);
    } catch(NoSuchDataAccessRequestException e) {
      log.error("Could not delete amendment {}", e);
    }

    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  public Response updateStatus(@QueryParam("to") String status) {
    return super.doUpdateStatus(id, status);
  }

  @GET
  @Timed
  @Path("/form/attachments/{attachmentName}/{attachmentId}/_download")
  public Response getFormAttachment(@PathParam("attachmentName") String attachmentName, @PathParam("attachmentId") String attachmentId) throws IOException {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    getService().findById(id);
    return Response.ok(fileStoreService.getFile(attachmentId)).header("Content-Disposition",
      "attachment; filename=\"" + attachmentName + "\"")
      .build();
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  protected DataAccessEntityService<DataAccessAmendment> getService() {
    return dataAccessAmendmentService;
  }

  private String getParentResourcePath() {
    return String.format("/data-access-request");
  }

  @Override
  String getResourcePath() {
    return String.format("/data-access-request/%s/amendment", parentId);
  }
}
