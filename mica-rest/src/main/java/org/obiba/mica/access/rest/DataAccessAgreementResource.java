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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.export.DataAccessEntityExporter;
import org.obiba.mica.access.service.DataAccessAgreementService;
import org.obiba.mica.access.service.DataAccessEntityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessAgreementForm;
import org.obiba.mica.micaConfig.service.DataAccessAgreementFormService;
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
public class DataAccessAgreementResource extends DataAccessEntityResource<DataAccessAgreement> {

  private static final Logger log = getLogger(DataAccessAgreementResource.class);

  private final Dtos dtos;

  private final DataAccessRequestService dataAccessRequestService;

  private final DataAccessAgreementService dataAccessAgreementService;

  private final DataAccessAgreementFormService dataAccessAgreementFormService;



  @Inject
  public DataAccessAgreementResource(
    SubjectAclService subjectAclService,
    FileStoreService fileStoreService,
    DataAccessConfigService dataAccessConfigService,
    Dtos dtos,
    DataAccessRequestService dataAccessRequestService,
    DataAccessAgreementService dataAccessAgreementService,
    DataAccessAgreementFormService dataAccessAgreementFormService,
    VariableSetService variableSetService,
    DataAccessRequestUtilService dataAccessRequestUtilService,
    SchemaFormConfigService schemaFormConfigService) {
    super(subjectAclService, fileStoreService, dataAccessConfigService, variableSetService, dataAccessRequestUtilService, schemaFormConfigService);
    this.dtos = dtos;
    this.dataAccessRequestService = dataAccessRequestService;
    this.dataAccessAgreementService = dataAccessAgreementService;
    this.dataAccessAgreementFormService = dataAccessAgreementFormService;
  }

  private String parentId;

  private String id;

  @GET
  @Timed
  public Mica.DataAccessRequestDto getAgreement() {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", parentId);
    DataAccessAgreement agreement = dataAccessAgreementService.findById(id);
    return dtos.asAgreementDto(agreement);
  }

  @GET
  @Path("/model")
  @Produces("application/json")
  public Map<String, Object> getModel() {
    subjectAclService.checkPermission(getResourcePath(), "VIEW", id);
    return JSONUtils.toMap(dataAccessAgreementService.findById(id).getContent());
  }

  @PUT
  @Path("/model")
  @Consumes("application/json")
  public Response setModel(String content) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessAgreement agreement = dataAccessAgreementService.findById(id);
    agreement.setContent(content);
    dataAccessAgreementService.save(agreement);
    return Response.ok().build();
  }

  @PUT
  @Timed
  public Response update(Mica.DataAccessRequestDto dto) {
    subjectAclService.checkPermission(getResourcePath(), "EDIT", id);
    if (!id.equals(dto.getId())) throw new BadRequestException();
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    DataAccessAgreement agreement = dtos.fromAgreementDto(dto);
    dataAccessAgreementService.save(agreement);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    String resource = getResourcePath();
    subjectAclService.checkPermission(resource, "DELETE", id);
    DataAccessRequest request = dataAccessRequestService.findById(parentId);
    if (request.isArchived()) throw new BadRequestException("Data access request is archived");

    try {
      dataAccessAgreementService.delete(id);
    } catch (NoSuchDataAccessRequestException e) {
      log.error("Could not delete end user agreement {}", e);
    }

    return Response.noContent().build();
  }

  @GET
  @Timed
  @Path("/_word")
  public Response getWordDocument(@QueryParam("lang") String lang) throws IOException {
    subjectAclService.checkPermission(getParentResourcePath(), "VIEW", id);

    if (Strings.isNullOrEmpty(lang)) lang = LANGUAGE_TAG_UNDETERMINED;

    DataAccessAgreement entity = dataAccessAgreementService.findById(id);
    DataAccessAgreementForm form = dataAccessAgreementFormService.findByRevision(entity.hasFormRevision() ? entity.getFormRevision().toString() : "latest").get();
    SchemaFormConfig config = schemaFormConfigService.getConfig(form, entity, lang);
    DataAccessEntityExporter exporter = DataAccessEntityExporter.newBuilder().config(config).build();
    String title = schemaFormConfigService.getTranslator(lang).translate("data-access-config.agreement.schema-form.title");
    String status = schemaFormConfigService.getTranslator(lang).translate(entity.getStatus().toString());
    return Response.ok(exporter.export(title, status, id).toByteArray())
      .header("Content-Disposition", "attachment; filename=\"" + "data-access-request-agreement-" + id + ".docx" + "\"").build();
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
    DataAccessAgreement agreement = dataAccessAgreementService.findById(id);

    List<StatusChange> submissions = agreement.getSubmissions();

    Map<String, Map<String, List<Object>>> data = submissions.stream()
      .reduce((first, second) -> second)
      .map(change ->  dataAccessRequestUtilService.getContentDiff("data-access-agreement", change.getContent(), agreement.getContent(), locale))
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

  public void setId(String id) {
    this.id = id;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  protected DataAccessEntityService<DataAccessAgreement> getService() {
    return dataAccessAgreementService;
  }

  @Override
  protected int getFormLatestRevision() {
    Optional<DataAccessAgreementForm> form = dataAccessAgreementFormService.findByRevision("latest");
    return form.map(AbstractDataAccessEntityForm::getRevision).orElse(0);
  }

  private String getParentResourcePath() {
    return String.format("/data-access-request");
  }

  @Override
  String getResourcePath() {
    return String.format("/data-access-request/%s/agreement", parentId);
  }
}
