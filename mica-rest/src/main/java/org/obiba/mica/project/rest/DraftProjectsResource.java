/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.IndexProjectsEvent;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;

@Component
@Scope("request")
@Path("/draft")
public class DraftProjectsResource {

  @Inject
  private ProjectService projectService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private EventBus eventBus;

  @GET
  @Path("/projects")
  @Timed
  public List<Mica.ProjectDto> list() {
    return projectService.findAllProjects().stream()
        .filter(n -> subjectAclService.isPermitted("/draft/project", "VIEW", n.getId()))
        .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(n -> dtos.asDto(n, true)).collect(Collectors.toList());
  }

  @POST
  @Path("/projects")
  @Timed
  @RequiresPermissions("/draft/project:ADD")
  public Response create(Mica.ProjectDto projectDto, @Context UriInfo uriInfo) {
    Project project = dtos.fromDto(projectDto);

    projectService.save(project);
    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "project", project.getId()).build()).build();
  }

  @PUT
  @Path("/projects/_index")
  @Timed
  @RequiresPermissions("/draft/project:PUBLISH")
  public Response reIndex() {
    eventBus.post(new IndexProjectsEvent());
    return Response.noContent().build();
  }

  @Path("/project/{id}")
  public DraftProjectResource project(@PathParam("id") String id) {
    DraftProjectResource resource = applicationContext.getBean(DraftProjectResource.class);
    resource.setId(id);

    return resource;
  }
}
