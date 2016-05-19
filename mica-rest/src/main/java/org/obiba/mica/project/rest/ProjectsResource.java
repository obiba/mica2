package org.obiba.mica.project.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/draft")
public class ProjectsResource {

  @Inject
  private ProjectService projectService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/projects")
  @Timed
  public List<Mica.ProjectDto> list(@QueryParam("study") String studyId) {
    return projectService.findAllProjects().stream()
      .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(n -> dtos.asDto(n, true)).collect(Collectors.toList());
  }


}
