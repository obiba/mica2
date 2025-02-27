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
import com.google.common.base.Strings;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.export.DataAccessEntityExporter;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessFeasibilityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessFeasibilityFormService;
import org.obiba.mica.micaConfig.service.SchemaFormConfigService;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Scope("request")
@RequiresAuthentication
public class DataAccessFeasibilityResource extends DataAccessEntityResource<DataAccessFeasibility> {

  private static final Logger log = getLogger(DataAccessFeasibilityResource.class);

  private final Dtos dtos;

  private final DataAccessRequestService dataAccessRequestService;

  private final DataAccessFeasibilityService dataAccessFeasibilityService;

  private final DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  @Inject
  public DataAccessFeasibilityResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessConfigService dataAccessConfigService,
    Dtos dtos,
    DataAccessRequestService dataAccessRequestService,
    DataAccessFeasibilityService dataAccessFeasibilityService,
    DataAccessFeasibilityFormService dataAccessFeasibilityFormService,
    VariableSetService variableSetService,
    DataAccessRequestUtilService dataAccessRequestUtilService,
    SchemaFormConfigService schemaFormConfigService) {
    super(subjectAclService, fileStoreService, dataAccessConfigService, variableSetService, dataAccessRequestUtilService, schemaFormConfigService);
    this.dtos = dtos;
    this.dataAccessRequestService = dataAccessRequestService;
    this.dataAccessFeasibilityService = dataAccessFeasibilityService;
    this.dataAccessFeasibilityFormService = dataAccessFeasibilityFormService;
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
    if (!id.equals(dto.getId())) throw new BadRequestException();
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
    } catch (NoSuchDataAccessRequestException e) {
      log.error("Could not delete feasibility {}", e);
    }

    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/_word")
  public Response getWordDocument(@QueryParam("lang") String lang) throws IOException {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", id);

    if (Strings.isNullOrEmpty(lang)) lang = LANGUAGE_TAG_UNDETERMINED;

    DataAccessFeasibility entity = dataAccessFeasibilityService.findById(id);
    DataAccessFeasibilityForm form = dataAccessFeasibilityFormService.findByRevision(entity.hasFormRevision() ? entity.getFormRevision().toString() : "latest").get();
    SchemaFormConfig config = schemaFormConfigService.getConfig(form, entity, lang);
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder().config(config, dataAccessFeasibilityFormService.getExportWordConfig()).build();
    String title = schemaFormConfigService.getTranslator(lang).translate("data-access-config.feasibility.schema-form.title");
    String status = schemaFormConfigService.getTranslator(lang).translate(entity.getStatus().toString());
    return Response.ok(exporter.export(title, status, id).toByteArray())
      .header("Content-Disposition", "attachment; filename=\"" + "data-access-request-feasibility-" + id + ".docx" + "\"").build();
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
    DataAccessFeasibility feasibility = dataAccessFeasibilityService.findById(id);

    List<StatusChange> submissions = feasibility.getSubmissions();

    Map<String, Map<String, List<Object>>> data = submissions.stream()
      .reduce((first, second) -> second)
      .map(change ->  dataAccessRequestUtilService.getContentDiff("data-access-feasibility", change.getContent(), feasibility.getContent(), locale))
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
    DataAccessFeasibility feasibility = getService().findById(id);
    feasibility.setVariablesSet(createOrUpdateVariablesSet(feasibility));
    dataAccessFeasibilityService.save(feasibility);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/variables")
  public Response deleteVariablesSet() {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessFeasibility feasibility = getService().findById(id);
    if (feasibility.hasVariablesSet())
      variableSetService.delete(feasibility.getVariablesSet());
    return Response.noContent().build();
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

  @Override
  protected int getFormLatestRevision() {
    Optional<DataAccessFeasibilityForm> form = dataAccessFeasibilityFormService.findByRevision("latest");
    return form.map(AbstractDataAccessEntityForm::getRevision).orElse(0);
  }

  private String getParentResourcePath() {
    return String.format("/data-access-request");
  }

  @Override
  String getResourcePath() {
    return String.format("/data-access-request/%s/feasibility", parentId);
  }
}
