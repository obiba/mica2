package org.obiba.mica.project.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.AbstractGitPersistableResource;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.service.NoSuchProjectException;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class DraftProjectResource extends AbstractGitPersistableResource<ProjectState, Project> {

  @Inject
  private ProjectService projectService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  private String id;

  @Override
  protected String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @GET
  public Mica.ProjectDto get() {
    subjectAclService.checkPermission("/draft/project", "VIEW", id);
    return dtos.asDto(projectService.findById(id), true);
  }

  @PUT
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.ProjectDto projectDto) {
    subjectAclService.checkPermission("/draft/project", "EDIT", id);
    // ensure network exists
    projectService.findById(id);

    Project project = dtos.fromDto(projectDto);
    projectService.save(project);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  public Response index() {
    subjectAclService.checkPermission("/draft/project", "EDIT", id);
    projectService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    subjectAclService.checkPermission("/draft/project", "PUBLISH", id);
    projectService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    subjectAclService.checkPermission("/draft/project", "PUBLISH", id);
    projectService.publish(id, false);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    subjectAclService.checkPermission("/draft/project", "DELETE", id);
    try {
      projectService.delete(id);
    } catch (NoSuchProjectException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource network(@PathParam("fileId") String fileId) {
    subjectAclService.checkPermission("/draft/project", "VIEW", id);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    projectService.findById(id);
    return fileResource;
  }

  @PUT
  @Path("/_status")
  public Response toUnderReview(@QueryParam("value") String status) {
    subjectAclService.checkPermission("/draft/project", "EDIT", id);
    projectService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.ProjectDto getFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    subjectAclService.checkPermission("/draft/project", "VIEW", id);
    return dtos.asDto(projectService.getFromCommit(projectService.findDraft(id), commitId), true);
  }

  @Path("/permissions")
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/project", id);
    subjectAclResource.setFileResourceInstance("/draft/file", "/project/" + id);
    return subjectAclResource;
  }

  @Path("/accesses")
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/project", id);
    subjectAclResource.setFileResourceInstance("/file", "/project/" + id);
    return subjectAclResource;
  }

  @Override
  protected AbstractGitPersistableService<ProjectState, Project> getService() {
    return projectService;
  }
}
