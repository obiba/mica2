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
import com.google.common.collect.Maps;
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
import org.apache.commons.compress.utils.Lists;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.export.DataAccessEntityExporter;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessPreliminaryService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.service.SchemaFormContentFileService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.DataAccessPreliminaryFormService;
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
    DataAccessRequestUtilService dataAccessRequestUtilService,
    SchemaFormConfigService schemaFormConfigService,
    SchemaFormContentFileService schemaFormContentFileService) {
    super(subjectAclService, fileStoreService, dataAccessConfigService, variableSetService, dataAccessRequestUtilService, schemaFormConfigService, schemaFormContentFileService);
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

  @GET
  @Timed
  @Path("/_word")
  public Response getWordDocument(@QueryParam("lang") String lang) throws IOException {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", id);

    if (Strings.isNullOrEmpty(lang)) lang = LANGUAGE_TAG_UNDETERMINED;

    DataAccessPreliminary entity = dataAccessPreliminaryService.findById(id);
    DataAccessPreliminaryForm form = dataAccessPreliminaryFormService.findByRevision(entity.hasFormRevision() ? entity.getFormRevision().toString() : "latest").get();
    SchemaFormConfig config = schemaFormConfigService.getConfig(form, entity, lang);
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder().config(config, dataAccessPreliminaryFormService.getExportWordConfig()).build();
    String title = schemaFormConfigService.getTranslator(lang).translate("data-access-config.preliminary.schema-form.title");
    String status = schemaFormConfigService.getTranslator(lang).translate(entity.getStatus().toString());
    return Response.ok(exporter.export(title, status, id).toByteArray())
      .header("Content-Disposition", "attachment; filename=\"" + "data-access-request-preliminary-" + id + ".docx" + "\"").build();
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

  @GET
  @Timed
  @Path("/files/_download")
  public Response getAttachment() {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    return downloadEntityFiles(getService().findById(id), "data-access-preliminary");
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
  protected Response approve(String id) {
    Response response =  super.approve(id);
    if (dataAccessConfigService.getOrCreateConfig().isMergePreliminaryContentEnabled() && response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
      DataAccessRequest request = dataAccessRequestService.findById(parentId);
      // inject preliminary data into main's content, for prefilling the opened main form
      if (DataAccessEntityStatus.OPENED.equals(request.getStatus())) {
        DataAccessPreliminary preliminary = dataAccessPreliminaryService.findById(id);
        Map<String, Object> map = JSONUtils.toMap(preliminary.getContent());
        request.setContent(JSONUtils.toJSON(removeObibaFilesValues(map)));
        dataAccessRequestService.save(request);
      }
    }
    return response;
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

  /**
   * File attachments cannot be transfered. Then remove them from the pre-fill data.
   *
   * @param map
   * @return
   */
  private Map<String, Object> removeObibaFilesValues(Map<String, Object> map) {
    Map<String, Object> cleanMap = Maps.newLinkedHashMap();
    for (String key : map.keySet()) {
      if (!"obibaFiles".equals(key)) {
        Object valueObj = map.get(key);
        cleanMap.put(key, removeObibaFilesValues(valueObj));
      }
    }
    return cleanMap;
  }

  private Object removeObibaFilesValues(Object valueObj) {
    if (valueObj instanceof Map) {
      Map<String, Object> values = (Map<String, Object>) valueObj;
      return removeObibaFilesValues(values);
    } else if (valueObj instanceof List) {
      List<Object> values = (List<Object>) valueObj;
      List<Object> cleanValues = Lists.newArrayList();
      for (Object value : values) {
        cleanValues.add(removeObibaFilesValues(value));
      }
      return cleanValues;
    }
    return valueObj;
  }
}
