package org.obiba.mica.study.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
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
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto) {
    // ensure study exists
    studyService.findDraftStudy(id);

    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
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
    FileResource studyResource = applicationContext.getBean(FileResource.class);
    studyResource.setPersistable(studyService.findDraftStudy(id));
    studyResource.setFileId(fileId);
    return studyResource;
  }
}
