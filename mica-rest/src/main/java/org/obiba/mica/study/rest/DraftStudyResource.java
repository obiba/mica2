package org.obiba.mica.study.rest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
import org.obiba.git.CommitInfo;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.study.domain.Study;
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
public class DraftStudyResource {

  @Inject
  private StudyService studyService;

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
    return dtos.asDto(studyService.findDraftStudy(id));
  }

  @PUT
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Nullable @QueryParam("comment") String comment) {
    // ensure study exists
    studyService.findDraftStudy(id);

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
    studyService.unpublish(id);
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
    Study study = studyService.findDraftStudy(id);

    if(study.findAttachmentById(fileId) == null) throw NoSuchEntityException.withId(Attachment.class, fileId);

    fileResource.setAttachment(study.findAttachmentById(fileId));

    return fileResource;
  }

  @Path("/commits")
  @GET
  public List<Mica.GitCommitInfoDto> getCommitsInfo() {
    return dtos.asDto(studyService.getCommitInfos(studyService.findDraftStudy(id)));
  }

  @PUT
  @RequiresPermissions({"/draft:EDIT"})
  @Path("/commit/{commitId}/restore")
  public Response restoreCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    Study study= studyService.getStudyFromCommit(studyService.findDraftStudy(id), commitId);
    if (study != null){
      studyService.save(study, createRestoreComment(studyService.getCommitInfo(study, commitId)));
    }

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}")
  public Mica.GitCommitInfoDto getCommitInfo(@NotNull @PathParam("commitId") String commitId) throws IOException {
    return dtos.asDto(
      getCommitInfoInternal(studyService.getCommitInfo(studyService.findDraftStudy(id), commitId), commitId, null));
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.StudyDto getStudyFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    return dtos.asDto(studyService.getStudyFromCommit(studyService.findDraftStudy(id), commitId));
  }

  private CommitInfo getCommitInfoInternal(@NotNull CommitInfo commitInfo, @NotNull String commitId,
    @Nullable String prevCommitId) {
    Iterable<String> diffEntries = studyService.getDiffEntries(studyService.findDraftStudy(id), commitId, prevCommitId);
    return CommitInfo.Builder.createFromObject(commitInfo).diffEntries((List<String>) diffEntries).build();
  }

  private String createRestoreComment(CommitInfo commitInfo) {
    LocalDateTime date = LocalDateTime
      .parse(commitInfo.getDate().toString(), DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss zzz yyyy"));
    String formatted =  date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a"));

    return String.format("Restored revision from '%s'", formatted);
  }
}
