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

import org.apache.shiro.authz.annotation.RequiresPermissions;
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
  @RequiresPermissions({"/draft:EDIT"})
  public Mica.StudyDto get() {
    return dtos.asDto(studyService.findDraft(id));
  }

  @PUT
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Nullable @QueryParam("comment") String comment) {
    // ensure study exists
    studyService.findDraft(id);

    Study study = dtos.fromDto(studyDto);
    studyService.save(study, comment);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  @Timed
  @RequiresPermissions({"/draft:PUBLISH"})
  public Response publish() {
    studyService.publish(id);
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  @RequiresPermissions({"/draft:PUBLISH"})
  public Response unPublish() {
    studyService.unPublish(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_status")
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Response toUnderReview(@QueryParam("value") String status) {
    studyService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));
    return Response.noContent().build();
  }

  /**
   * DELETE  /ws/studies/:id -> delete the "id" study.
   */
  @DELETE
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Response delete() {
    studyService.delete(id);
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  @RequiresPermissions({"/draft:EDIT"})
  public FileResource study(@PathParam("fileId") String fileId) {
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    Study study = studyService.findDraft(id);

    if (study.hasLogo() && study.getLogo().getId().equals(fileId)) {
      fileResource.setAttachment(study.getLogo());
    } else {
      List<Attachment> attachments = fileSystemService.findAttachments(String.format("^/study/%s", study.getId()), false).stream().filter(
        a -> a.getId().equals(fileId)).collect(Collectors.toList());
      if(attachments.isEmpty()) throw NoSuchEntityException.withId(Attachment.class, fileId);
      fileResource.setAttachment(attachments.get(0));
    }

    return fileResource;
  }

  @GET
  @RequiresPermissions({"/draft:EDIT"})
  @Path("/commit/{commitId}/view")
  public Mica.StudyDto getStudyFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
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
