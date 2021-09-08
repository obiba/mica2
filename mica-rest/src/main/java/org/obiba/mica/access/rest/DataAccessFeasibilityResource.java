package org.obiba.mica.access.rest;


import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessFeasibilityService;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.dataset.service.VariableSetService;
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
import java.io.IOException;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessFeasibilityResource extends DataAccessEntityResource<DataAccessFeasibility> {

  private static final Logger log = getLogger(DataAccessFeasibilityResource.class);

  private final Dtos dtos;

  private final DataAccessRequestService dataAccessRequestService;

  private final DataAccessFeasibilityService dataAccessFeasibilityService;

  @Inject
  public DataAccessFeasibilityResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessFormService dataAccessFormService,
    Dtos dtos,
    DataAccessRequestService dataAccessRequestService,
    DataAccessFeasibilityService dataAccessFeasibilityService,
    VariableSetService variableSetService) {
    super(subjectAclService, fileStoreService, dataAccessFormService, variableSetService);
    this.dtos = dtos;
    this.dataAccessRequestService = dataAccessRequestService;
    this.dataAccessFeasibilityService = dataAccessFeasibilityService;
  }

  private String parentId;

  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto getFeasibility() {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", parentId);
    DataAccessFeasibility feasibility = dataAccessFeasibilityService.findById(id);
    return dtos.asFeasibilityDto(feasibility);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    return JSONUtils.toMap(dataAccessFeasibilityService.findById(id).getContent());
  }

  @PUT
  @Path("/model")
  @Consumes("application/json")
  public Response setModel(String content) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessFeasibility feasibility = dataAccessFeasibilityService.findById(id);
    feasibility.setContent(content);
    dataAccessFeasibilityService.save(feasibility);
    return Response.ok().build();
  }

  @PUT
  @Timed
  public Response update(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    if(!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessFeasibility feasibility = dtos.fromFeasibilityDto(dto);
    dataAccessFeasibilityService.save(feasibility);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    String resource = getResourcePath();
    subjectAclService.checkPermission(resource, "DELETE", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    try {
      dataAccessFeasibilityService.delete(id);
    } catch(NoSuchDataAccessRequestException e) {
      log.error("Could not delete feasibility {}", e);
    }

    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  public Response updateStatus(@QueryParam("to") String status) {
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");
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
  protected DataAccessEntityService<DataAccessFeasibility> getService() {
    return dataAccessFeasibilityService;
  }

  private String getParentResourcePath() {
    return String.format("/data-access-request");
  }

  @Override
  String getResourcePath() {
    return String.format("/data-access-request/%s/feasibility", parentId);
  }
}
