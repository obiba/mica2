package org.obiba.mica.study.rest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing draft Study.
 */
@Component
@Scope("request")
public class DraftStudyResource extends AbstractGitPersistableResource<StudyState, Study> {

  @Inject
  private StudyService studyService;

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
  public Mica.StudyDto get() {
    subjectAclService.checkPermission("/draft/study", "VIEW", id);
    return dtos.asDto(studyService.findDraft(id), true);
  }

  @PUT
  @Timed
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto,
    @Nullable @QueryParam("comment") String comment) {
    subjectAclService.checkPermission("/draft/study", "EDIT", id);
    // ensure study exists
    studyService.findDraft(id);

    Study study = dtos.fromDto(studyDto);
    studyService.save(study, comment);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  @Timed
  public Response publish() {
    subjectAclService.checkPermission("/draft/study", "PUBLISH", id);
    studyService.publish(id);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    subjectAclService.checkPermission("/draft/study", "PUBLISH", id);
    studyService.unPublish(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  @Timed
  public Response toUnderReview(@QueryParam("value") String status) {
    subjectAclService.checkPermission("/draft/study", "EDIT", id);
    studyService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));
    return Response.noContent().build();
  }

  /**
   * DELETE  /ws/studies/:id -> delete the "id" study.
   */
  @DELETE
  @Timed
  public Response delete() {
    subjectAclService.checkPermission("/draft/study", "DELETE", id);
    studyService.delete(id);
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource study(@PathParam("fileId") String fileId) {
    subjectAclService.checkPermission("/draft/study", "VIEW", id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Study study = studyService.findDraft(id);

    if(study.hasLogo() && study.getLogo().getId().equals(fileId)) {
      fileResource.setAttachment(study.getLogo());
    } else {
      List<Attachment> attachments = fileSystemService
        .findAttachments(String.format("^/study/%s", study.getId()), false).stream()
        .filter(a -> a.getId().equals(fileId)).collect(Collectors.toList());
      if(attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
      fileResource.setAttachment(attachments.get(0));
    }

    return fileResource;
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.StudyDto getStudyFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    subjectAclService.checkPermission("/draft/study", "VIEW", id);
    return dtos.asDto(studyService.getFromCommit(studyService.findDraft(id), commitId));
  }

  @Override
  protected String getId() {
    return id;
  }

  @Override
  protected AbstractGitPersistableService<StudyState, Study> getService() {
    return studyService;
  }
}
