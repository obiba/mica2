/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;


import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessPreliminaryService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessPreliminaryFormService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessPreliminaryResource extends DataAccessEntityResource<DataAccessPreliminary> {

  private static final Logger log = getLogger(DataAccessPreliminaryResource.class);

  private final Dtos dtos;

  private final DataAccessRequestService dataAccessRequestService;

  private final DataAccessPreliminaryService dataAccessPreliminaryService;

  private final DataAccessPreliminaryFormService dataAccessPreliminaryFormService;

  @Inject
  public DataAccessPreliminaryResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessConfigService dataAccessConfigService,
    Dtos dtos,
    DataAccessRequestService dataAccessRequestService,
    DataAccessPreliminaryService dataAccessPreliminaryService,
    DataAccessPreliminaryFormService dataAccessPreliminaryFormService,
    VariableSetService variableSetService,
    DataAccessRequestUtilService dataAccessRequestUtilService) {
    super(subjectAclService, fileStoreService, dataAccessConfigService, variableSetService, dataAccessRequestUtilService);
    this.dtos = dtos;
    this.dataAccessRequestService = dataAccessRequestService;
    this.dataAccessPreliminaryService = dataAccessPreliminaryService;
    this.dataAccessPreliminaryFormService = dataAccessPreliminaryFormService;
  }

  private String parentId;

  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto getPreliminary() {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", id);
    DataAccessPreliminary preliminary = dataAccessPreliminaryService.getOrCreate(id);
    return dtos.asPreliminaryDto(preliminary);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    return JSONUtils.toMap(dataAccessPreliminaryService.findById(id).getContent());
  }

  @PUT
  @Path("/model")
  @Consumes("application/json")
  public Response setModel(String content) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessPreliminary preliminary = dataAccessPreliminaryService.findById(id);
    preliminary.setContent(content);
    dataAccessPreliminaryService.save(preliminary);
    return Response.ok().build();
  }

  @PUT
  @Timed
  public Response update(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    if (!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessPreliminary preliminary = dtos.fromPreliminaryDto(dto);
    dataAccessPreliminaryService.save(preliminary);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    String resource = getResourcePath();
    subjectAclService.checkPermission(resource, "DELETE", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    try {
      dataAccessPreliminaryService.delete(id);
    } catch (NoSuchDataAccessRequestException e) {
      log.error("Could not delete preliminary {}", e);
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
  @Path("/_diff")
  public Response diffStatusChanges(@QueryParam("locale") @DefaultValue("en") String locale) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessPreliminary preliminary = dataAccessPreliminaryService.findById(id);

    List<StatusChange> submissions = preliminary.getSubmissions();

    Map<String, Map<String, List<Object>>> data = submissions.stream()
      .reduce((first, second) -> second)
      .map(change ->  dataAccessRequestUtilService.getContentDiff("data-access-preliminary", change.getContent(), preliminary.getContent(), locale))
      .orElse(null);

    return Response.ok(data, MediaType.APPLICATION_JSON_TYPE).build();
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

  @PUT
  @Path("/variables")
  public Response setVariablesSet() {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessPreliminary preliminary = getService().findById(id);
    preliminary.setVariablesSet(createOrUpdateVariablesSet(preliminary));
    dataAccessPreliminaryService.save(preliminary);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/variables")
  public Response deleteVariablesSet() {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessPreliminary preliminary = getService().findById(id);
    if (preliminary.hasVariablesSet())
      variableSetService.delete(preliminary.getVariablesSet());
    return Response.noContent().build();
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
    this.id = parentId;
  }

  @Override
  protected DataAccessEntityService<DataAccessPreliminary> getService() {
    return dataAccessPreliminaryService;
  }

  @Override
  protected int getFormLatestRevision() {
    Optional<DataAccessPreliminaryForm> form = dataAccessPreliminaryFormService.findByRevision("latest");
    return form.map(AbstractDataAccessEntityForm::getRevision).orElse(0);
  }

  private String getParentResourcePath() {
    return String.format("/data-access-request");
  }

  @Override
  String getResourcePath() {
    return String.format("/data-access-request/%s/preliminary", parentId);
  }

  @Override
  protected Response reject(String id) {
    Response response = super.reject(id);
    if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
      DataAccessRequest request = dataAccessRequestService.findById(parentId);
      dataAccessRequestService.updateStatus(parentId, DataAccessEntityStatus.REJECTED);
      applyApplicantNotEditablePermissions(request.getApplicant(), "/data-access-request", parentId);
    }
    return response;
  }
}
