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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.PublishedProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/")
@RequiresAuthentication
public class PublishedProjectsResource {

  @Inject
  private PublishedProjectService publishedProjectService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private SubjectAclService subjectAclService;

  @GET
  @Path("/projects")
  @Timed
  public Mica.ProjectsDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("query") String query) {

    PublishedDocumentService.Documents<Project> projects = publishedProjectService
        .find(from, limit, sort, order, null, query);

    Mica.ProjectsDto.Builder builder = Mica.ProjectsDto.newBuilder();

    builder.setFrom(projects.getFrom()).setLimit(projects.getLimit()).setTotal(projects.getTotal());
    builder.addAllProjects(projects.getList().stream().map(dtos::asDto).collect(Collectors.toList()));

    return builder.build();
  }

  @GET
  @Path("/projects/dar_accessible")
  @Timed
  public Mica.ProjectsDto listByDataAccessRequest() {
    List<Mica.ProjectDto> projectDtoList = publishedProjectService.findAll().stream()
      .map(dtos::asDto)
      .filter(p -> p.getRequest().isInitialized() && subjectAclService.isPermitted("/data-access-request", "VIEW", p.getRequest().getId()))
      .collect(Collectors.toList());

    Mica.ProjectsDto.Builder builder = Mica.ProjectsDto.newBuilder();
    builder.setFrom(0).setLimit(projectDtoList.size()).setTotal(projectDtoList.size());
    builder.addAllProjects(projectDtoList);

    return builder.build();
  }

  @Path("/project/{id}")
  public PublishedProjectResource project(@PathParam("id") String id) {
    PublishedProjectResource projectResource = applicationContext.getBean(PublishedProjectResource.class);
    projectResource.setId(id);
    return projectResource;
  }

}
