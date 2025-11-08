/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.study.ConstraintException;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing draft Study.
 */
@Component
@Scope("request")
public class DraftHarmonizationStudyResource extends AbstractGitPersistableResource<HarmonizationStudyState, HarmonizationStudy> {

  @Inject
  private HarmonizationStudyService studyService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudyDto get(@QueryParam("key") String key,
                           @QueryParam("participatingStudies") @DefaultValue("false") boolean participatingStudies) {
    checkPermission("/draft/harmonization-study", "VIEW", key);
    List<HarmonizationDataset> datasets = participatingStudies
      ? harmonizedDatasetService.findAllDatasetsByHarmonizationStudy(id)
      : Lists.newArrayList();

    return dtos.asDto(studyService.findDraft(id), true, datasets);
  }

  @GET
  @Path("/model")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getModel() {
    checkPermission("/draft/harmonization-study", "VIEW");
    return studyService.findDraft(id).getModel();
  }

  @PUT
  @Path("/model")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateModel(String body) {
    checkPermission("/draft/harmonization-study", "EDIT");
    HarmonizationStudy study = studyService.findDraft(id);
    study.setModel(Strings.isNullOrEmpty(body) ? new HashMap<>() : JSONUtils.toMap(body));
    studyService.save(study);
    return Response.ok().build();
  }

  @GET
  @Path("/attributes")
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Attribute> getAttributes() {
    checkPermission("/draft/harmonization-study", "VIEW");
    return studyService.findDraft(id).getAttributes();
  }

  @PUT
  @Path("/attributes")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateAttributes(Set<Attribute> attributes) {
    checkPermission("/draft/harmonization-study", "EDIT");
    HarmonizationStudy study = studyService.findDraft(id);
    study.setAttributes(attributes);
    studyService.save(study);
    return Response.ok().build();
  }

  @GET
  @Timed
  @Path("/export_csv")
  @Produces("text/csv")
  public Response exportAsCsv() throws JsonProcessingException {
    return Response.ok(studyService.writeCsv(getId()).toByteArray(), "text/csv")
      .header("Content-Disposition", "attachment; filename=\"" + getId() + ".csv\"").build();
  }

  @PUT
  @Timed
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto,
                         @Nullable @QueryParam("comment") String comment) {
    checkPermission("/draft/harmonization-study", "EDIT");
    // ensure study exists
    studyService.findDraft(id);

    HarmonizationStudy study = (HarmonizationStudy) dtos.fromDto(studyDto);

    HashMap<Object, Object> response = Maps.newHashMap();
    response.put("study", study);

    studyService.save(study, comment);
    return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @PUT
  @Path("/_publish")
  @Timed
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    checkPermission("/draft/harmonization-study", "PUBLISH");
    studyService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    checkPermission("/draft/harmonization-study", "PUBLISH");

    Map<String, List<String>> conflicts =
      studyService.getPotentialUnpublishingConflicts(studyService.findStudy(id));

    if (!conflicts.isEmpty()) {
      throw new ConstraintException(conflicts);
    }

    studyService.publish(id, false);
    return Response.ok().build();
  }

  @PUT
  @Path("/_status")
  @Timed
  public Response toUnderReview(@QueryParam("value") String status) {
    checkPermission("/draft/harmonization-study", "EDIT");
    studyService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));
    return Response.noContent().build();
  }

  /**
   * {@code DELETE  /ws/studies/:id -> delete the "id" study.}
   */
  @DELETE
  @Timed
  public Response delete() {
    checkPermission("/draft/harmonization-study", "DELETE");
    studyService.delete(id);
    removeExternalEditorPermissionsIfApplicable("/draft/harmonization-study");
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource file(@PathParam("fileId") String fileId, @QueryParam("key") String key) {
    checkPermission("/draft/harmonization-study", "VIEW", key);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    HarmonizationStudy study = studyService.findDraft(id);

    if (study.hasLogo() && study.getLogo().getId().equals(fileId)) {
      fileResource.setAttachment(study.getLogo());
    } else {
      List<Attachment> attachments = fileSystemService
        .findAttachments(String.format("^/harmonization-study/%s", study.getId()), false).stream()
        .filter(a -> a.getId().equals(fileId)).collect(Collectors.toList());
      if (attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
      fileResource.setAttachment(attachments.get(0));
    }

    return fileResource;
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.StudyDto getStudyFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/harmonization-study", "VIEW");
    return dtos.asDto(studyService.getFromCommit(studyService.findDraft(id), commitId), true);
  }

  @Path("/permissions")
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/harmonization-study", id);
    subjectAclResource.setFileResourceInstance("/draft/file", "/harmonization-study/" + id);
    return subjectAclResource;
  }

  @Path("/accesses")
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/harmonization-study", id);
    subjectAclResource.setFileResourceInstance("/file", "/harmonization-study/" + id);
    return subjectAclResource;
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  protected AbstractGitPersistableService<HarmonizationStudyState, HarmonizationStudy> getService() {
    return studyService;
  }
}
