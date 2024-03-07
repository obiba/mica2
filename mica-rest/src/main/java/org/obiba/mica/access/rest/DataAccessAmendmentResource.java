package org.obiba.mica.access.rest;


import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.export.DataAccessEntityExporter;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.SchemaFormConfigService;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
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
public class DataAccessAmendmentResource extends DataAccessEntityResource<DataAccessAmendment> {

  private static final Logger log = getLogger(DataAccessAmendmentResource.class);

  private final Dtos dtos;

  private final DataAccessRequestService dataAccessRequestService;

  private final DataAccessAmendmentService dataAccessAmendmentService;

  private final DataAccessAmendmentFormService dataAccessAmendmentFormService;

  @Inject
  public DataAccessAmendmentResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessConfigService dataAccessConfigService,
    Dtos dtos,
    DataAccessRequestService dataAccessRequestService,
    DataAccessAmendmentService dataAccessAmendmentService,
    DataAccessAmendmentFormService dataAccessAmendmentFormService,
    VariableSetService variableSetService,
    DataAccessRequestUtilService dataAccessRequestUtilService,
    SchemaFormConfigService schemaFormConfigService) {
    super(subjectAclService, fileStoreService, dataAccessConfigService, variableSetService, dataAccessRequestUtilService, schemaFormConfigService);
    this.dtos = dtos;
    this.dataAccessRequestService = dataAccessRequestService;
    this.dataAccessAmendmentService = dataAccessAmendmentService;
    this.dataAccessAmendmentFormService = dataAccessAmendmentFormService;
  }

  private String parentId;

  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto getAmendment() {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", parentId);
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    return dtos.asAmendmentDto(amendment);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    return JSONUtils.toMap(dataAccessAmendmentService.findById(id).getContent());
  }

  @PUT
  @Path("/model")
  @Consumes("application/json")
  public Response setModel(String content) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    amendment.setContent(content);
    dataAccessAmendmentService.save(amendment);
    return Response.ok().build();
  }

  @PUT
  @Timed
  public Response update(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    if (!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessAmendment amendment = dtos.fromAmendmentDto(dto);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    dataAccessAmendmentService.save(amendment);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    String resource = getResourcePath();
    subjectAclService.checkPermission(resource, "DELETE", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    try {
      dataAccessAmendmentService.delete(id);
    } catch (NoSuchDataAccessRequestException e) {
      log.error("Could not delete amendment {}", e);
    }

    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/_word")
  public Response getWordDocument(@QueryParam("lang") String lang) throws IOException {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", id);

    if (Strings.isNullOrEmpty(lang)) lang = LANGUAGE_TAG_UNDETERMINED;

    DataAccessAmendment entity = dataAccessAmendmentService.findById(id);
    DataAccessAmendmentForm form = dataAccessAmendmentFormService.findByRevision(entity.hasFormRevision() ? entity.getFormRevision().toString() : "latest").get();
    SchemaFormConfig config = schemaFormConfigService.getConfig(form, entity, lang);
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder().config(config).build();
    String title = schemaFormConfigService.getTranslator(lang).translate("data-access-config.amendment.schema-form.title");
    String status = schemaFormConfigService.getTranslator(lang).translate(entity.getStatus().toString());
    return Response.ok(exporter.export(title, status, id).toByteArray())
      .header("Content-Disposition", "attachment; filename=\"" + "data-access-request-amendment-" + id + ".docx" + "\"").build();
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
    subjectAclService.checkPermission(getParentResourcePath(), "EDIT", parentId);
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);

    List<StatusChange> submissions = amendment.getSubmissions();

    Map<String, Map<String, List<Object>>> data = submissions.stream()
      .reduce((first, second) -> second)
      .map(change ->  dataAccessRequestUtilService.getContentDiff("data-access-amendment", change.getContent(), amendment.getContent(), locale))
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
    DataAccessAmendment amendment = getService().findById(id);
    amendment.setVariablesSet(createOrUpdateVariablesSet(amendment));
    dataAccessAmendmentService.save(amendment);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/variables")
  public Response deleteVariablesSet() {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessAmendment amendment = getService().findById(id);
    if (amendment.hasVariablesSet())
      variableSetService.delete(amendment.getVariablesSet());
    return Response.noContent().build();
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

  @Override
  protected int getFormLatestRevision() {
    Optional<DataAccessAmendmentForm> form = dataAccessAmendmentFormService.findByRevision("latest");
    return form.map(AbstractDataAccessEntityForm::getRevision).orElse(0);
  }

  private String getParentResourcePath() {
    return String.format("/data-access-request");
  }

  @Override
  String getResourcePath() {
    return String.format("/data-access-request/%s/amendment", parentId);
  }
}
