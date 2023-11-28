/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.rest;

import com.google.common.base.Strings;
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

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
  public Mica.ProjectDto get(@QueryParam("key") String key) {
    checkPermission("/draft/project", "VIEW", key);
    return dtos.asDto(projectService.findById(id), true);
  }

  @GET
  @Path("/model")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getModel() {
    checkPermission("/draft/project", "VIEW");
    return projectService.findById(id).getModel();
  }

  @PUT
  @Path("/model")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateModel(String body) {
    checkPermission("/draft/project", "EDIT");
    Project project = projectService.findById(id);
    project.setModel(Strings.isNullOrEmpty(body) ? new HashMap<>() : JSONUtils.toMap(body));
    projectService.save(project);
    return Response.ok().build();
  }

  @PUT
  public Response update(@SuppressWarnings("TypeMayBeWeakened") Mica.ProjectDto projectDto,
                         @Nullable @QueryParam("comment") String comment) {
    checkPermission("/draft/project", "EDIT");
    // ensure network exists
    projectService.findById(id);

    Project project = dtos.fromDto(projectDto);
    projectService.save(project, comment);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_index")
  public Response index() {
    checkPermission("/draft/project", "EDIT");
    projectService.index(id);
    return Response.noContent().build();
  }

  @PUT
  @Path("/_publish")
  public Response publish(@QueryParam("cascading") @DefaultValue("UNDER_REVIEW") String cascadingScope) {
    checkPermission("/draft/project", "PUBLISH");
    projectService.publish(id, true, PublishCascadingScope.valueOf(cascadingScope.toUpperCase()));
    return Response.noContent().build();
  }

  @DELETE
  @Path("/_publish")
  public Response unPublish() {
    checkPermission("/draft/project", "PUBLISH");
    projectService.publish(id, false);
    return Response.noContent().build();
  }

  @DELETE
  public Response delete() {
    checkPermission("/draft/project", "DELETE");
    try {
      projectService.delete(id);
      removeExternalEditorPermissionsIfApplicable("/draft/project");
    } catch (NoSuchProjectException e) {
      // ignore
    }
    return Response.noContent().build();
  }

  @Path("/file/{fileId}")
  public FileResource file(@PathParam("fileId") String fileId, @QueryParam("key") String key) {
    checkPermission("/draft/project", "VIEW", key);
    FileResource fileResource = applicationContext.getBean(FileResource.class);
    projectService.findById(id);
    return fileResource;
  }

  @PUT
  @Path("/_status")
  public Response toUnderReview(@QueryParam("value") String status) {
    checkPermission("/draft/project", "EDIT");
    projectService.updateStatus(id, RevisionStatus.valueOf(status.toUpperCase()));

    return Response.noContent().build();
  }

  @GET
  @Path("/commit/{commitId}/view")
  public Mica.ProjectDto getFromCommit(@NotNull @PathParam("commitId") String commitId) throws IOException {
    checkPermission("/draft/project", "VIEW");
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
