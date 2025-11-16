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

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.EntityStateFilter;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.IndexProjectsEvent;
import org.obiba.mica.project.service.DraftProjectService;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.search.AccessibleIdFilterBuilder;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@Scope("request")
@Path("/draft")
public class DraftProjectsResource {
  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

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

  @Inject
  private DraftProjectService draftProjectService;

  @GET
  @Path("/projects")
  @Timed
  public Mica.ProjectsDto list(@QueryParam("query") String query,
                               @QueryParam("from") @DefaultValue("0") Integer from,
                               @QueryParam("limit") Integer limit,
                               @QueryParam("sort") @DefaultValue("id") String sort,
                               @QueryParam("order") @DefaultValue("asc") String order,
                               @QueryParam("filter") @DefaultValue("ALL") String filter,
                               @Context HttpServletResponse response) {

    EntityStateFilter entityStateFilter = EntityStateFilter.valueOf(filter);
    List<String> filteredIds = projectService.getIdsByStateFilter(entityStateFilter);

    Searcher.IdFilter accessibleIdFilter = AccessibleIdFilterBuilder.newBuilder()
      .aclService(subjectAclService)
      .resources(Lists.newArrayList("/draft/project"))
      .ids(filteredIds)
      .build();

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    DocumentService.Documents<Project> projectDocuments = draftProjectService.find(from, limit, sort, order,
      null, query, null, null, accessibleIdFilter);
    long totalCount = projectDocuments.getTotal();

    List<Mica.ProjectDto> result = projectDocuments.getList()
      .stream()
      .map(n -> dtos.asDto(n, true))
      .collect(toList());

    Mica.ProjectsDto.Builder builder = Mica.ProjectsDto.newBuilder();
    builder.setFrom(from).setLimit(limit).setTotal(Long.valueOf(totalCount).intValue());
    builder.addAllProjects(result);

    if (subjectAclService.isPermitted("/draft/project", "ADD")) {
      builder.addActions("ADD");
    }

    return builder.build();
  }

  @POST
  @Path("/projects")
  @Timed
  @RequiresPermissions("/draft/project:ADD")
  public Response create(Mica.ProjectDto projectDto, @Context UriInfo uriInfo,
                         @Nullable @QueryParam("comment") String comment) {
    Project project = dtos.fromDto(projectDto);

    projectService.save(project, comment);

    if (SecurityUtils.getSubject().hasRole(Roles.MICA_EXTERNAL_EDITOR)) {
      subjectAclService.addPermission("/draft/project", "VIEW,EDIT", project.getId());
      subjectAclService.addPermission("/draft/project/" + project.getId(), "EDIT", "_status");
      subjectAclService.addPermission("/draft/project/" + project.getId() + "/_attachments", "EDIT");
    }

    return Response.created(uriInfo.getBaseUriBuilder().segment("draft", "project", project.getId()).build()).build();
  }

  @PUT
  @Path("/projects/_index")
  @Timed
  @RequiresPermissions("/draft/project:PUBLISH")
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    eventBus.post(new IndexProjectsEvent(ids));
    return Response.noContent().build();
  }

  @Path("/project/{id}")
  public DraftProjectResource project(@PathParam("id") String id) {
    DraftProjectResource resource = applicationContext.getBean(DraftProjectResource.class);
    resource.setId(id);

    return resource;
  }
}
