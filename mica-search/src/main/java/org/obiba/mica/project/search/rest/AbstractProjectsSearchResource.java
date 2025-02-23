/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.search.rest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;

import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.search.EsPublishedProjectService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.codahale.metrics.annotation.Timed;

public abstract class AbstractProjectsSearchResource {


  @Inject
  private Dtos dtos;

  @Inject
  private EsPublishedProjectService esProjectService;

  protected abstract boolean isDraft();

  @GET
  @Timed
  public Mica.ProjectsDto query(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") @DefaultValue("asc") String order, @QueryParam("query") String query) throws IOException {

    PublishedDocumentService.Documents<Project> projects = esProjectService.find(from, limit, sort, order, null, query);

    List<Mica.ProjectDto> projectDtos = projects.getList().stream().map(p -> dtos.asDto(p, isDraft()))
      .collect(Collectors.toList());

    Mica.ProjectsDto.Builder builder = Mica.ProjectsDto.newBuilder().setFrom(from).setLimit(limit)
      .setTotal(projectDtos.size());

    builder.addAllProjects(projectDtos);

    return builder.build();
  }
}
