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
import com.google.common.collect.Maps;
import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.JSONUtils;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.study.ConstraintException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing draft Study.
 */
@Component
@Scope("request")
public class DraftIndividualStudyResource extends AbstractGitPersistableResource<StudyState, Study> {

  @Inject
  private IndividualStudyService individualStudyService;

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
  public Mica.StudyDto get(@QueryParam("locale") String locale, @QueryParam("key") String key) {
    checkPermission("/draft/individual-study", "VIEW", key);
    return dtos.asDto(individualStudyService.findDraft(id, locale), true);
  }

  @GET
  @Path("/model")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getModel() {
    checkPermission("/draft/individual-study", "VIEW");
    return individualStudyService.findDraft(id).getModel();
  }

  @PUT
  @Path("/model")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateModel(String body) {
    checkPermission("/draft/individual-study", "EDIT");
    Study study = individualStudyService.findDraft(id);
    study.setModel(Strings.isNullOrEmpty(body) ? new HashMap<>() : JSONUtils.toMap(body));
    individualStudyService.save(study);
    return Response.ok().build();
  }

  @GET
  @Timed
  @Path("/export_csv")
  @Produces("text/csv")
  public Response exportAsCsv() throws JsonProcessingException {
    return Response.ok(individualStudyService.writeCsv(getId()).toByteArray(), "text/csv")
        .header("Content-Disposition", "attachment; filename=\"" + getId() + ".csv\"").build();
  }

  @PUT
  @Timed
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto,
      @Nullable @QueryParam("comment") String comment, @QueryParam("weightChanged") boolean weightChanged) {
    checkPermission("/draft/individual-study", "EDIT");
    // ensure study exists
    individualStudyService.findDraft(id);

    Study study = (Study) dtos.fromDto(studyDto);

    HashMap<Object, Object> response = Maps.newHashMap();
    response.put("study", study);
    response.put("potentialConflicts", individualStudyService.getPotentialConflicts(study, false));

    individualStudyService.save(study, comment, weightChanged);
    return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @PUT
  @Path("/_publish")
  @Timed
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    checkPermission("/draft/individual-study", "PUBLISH");
    individualStudyService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    checkPermission("/draft/individual-study", "PUBLISH");

    Map<String, List<String>> conflicts =
      individualStudyService.getPotentialUnpublishingConflicts(individualStudyService.findStudy(id));

    if (!conflicts.isEmpty()) {
      throw new ConstraintException(conflicts);
    }

    individualStudyService.publish(id, false);

    return Response.ok().build();
  }

  @PUT
  @Path("/_status")
  @Timed
  public Response toUnderReview(@QueryParam("value") String status) {
    checkPermission("/draft/individual-study", "EDIT");
    individualStudyService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));
    return Response.noContent().build();
  }

  /**
   * DELETE  /ws/studies/:id -> delete the "id" study.
   */
  @DELETE
  @Timed
  public Response delete() {
    checkPermission("/draft/individual-study", "DELETE");
    individualStudyService.delete(id);
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource file(@PathParam("fileId") String fileId, @QueryParam("key") String key) {
    checkPermission("/draft/individual-study", "VIEW", key);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Study study = individualStudyService.findDraft(id, null); // must compare un-cached study with un-cached study; findDraft(String) is cached

    if (study.hasLogo() && study.getLogo().getId().equals(fileId)) {
      fileResource.setAttachment(study.getLogo());
    } else {
      List<Attachment> attachments = fileSystemService
        .findAttachments(String.format("^/individual-study/%s", study.getId()), false).stream()
        .filter(a -> a.getId().equals(fileId)).collect(Collectors.toList());
      if (attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
      fileResource.setAttachment(attachments.get(0));
    }

    return fileResource;
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.StudyDto getStudyFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/individual-study", "VIEW");
    return dtos.asDto(individualStudyService.getFromCommit(individualStudyService.findDraft(id), commitId), true);
  }

  @Path("/permissions")
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/individual-study", id);
    subjectAclResource.setFileResourceInstance("/draft/file", "/individual-study/" + id);
    return subjectAclResource;
  }

  @Path("/accesses")
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/individual-study", id);
    subjectAclResource.setFileResourceInstance("/file", "/individual-study/" + id);
    return subjectAclResource;
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  protected AbstractGitPersistableService<StudyState, Study> getService() {
    return individualStudyService;
  }
}
