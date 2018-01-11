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
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.IndexProjectsEvent;
import org.obiba.mica.project.service.DraftProjectService;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
                                    @QueryParam("limit") Integer limit, @Context HttpServletResponse response) {
    Stream<Project> result;
    long totalCount;

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    if(Strings.isNullOrEmpty(query)) {
      List<Project> projects = projectService.findAllProjects().stream()
        .filter(n -> subjectAclService.isPermitted("/draft/project", "VIEW", n.getId())).collect(toList());
      totalCount = projects.size();
      result = projects.stream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).skip(from).limit(limit);
    } else {
      DocumentService.Documents<Project> projectDocuments = draftProjectService.find(from, limit, null, null, null, query);
      totalCount = projectDocuments.getTotal();
      result = projectService.findAllProjects(projectDocuments.getList().stream().map(AbstractGitPersistable::getId).collect(toList())).stream();
    }

    Mica.ProjectsDto.Builder builder = Mica.ProjectsDto.newBuilder();
    builder.setFrom(from).setLimit(limit).setTotal(Long.valueOf(totalCount).intValue());
    builder.addAllProjects(result.map(n -> dtos.asDto(n, true)).collect(toList()));

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
