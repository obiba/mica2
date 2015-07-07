package org.obiba.mica.study.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Projects;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing draft Study state.
 */
@Component
@Scope("request")
public class StudyStateResource {

  @Inject
  private StudyService studyService;

  @Inject
  private Dtos dtos;

  @Inject
  private OpalService opalService;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  @RequiresPermissions({"/draft:EDIT"})
  public Mica.StudySummaryDto get() {
    return dtos.asDto(studyService.findStateById(id));
  }

  @PUT
  @Path("/_publish")
  @RequiresPermissions({"/draft:PUBLISH"})
  public Response publish() {
    studyService.publish(id);
    return Response.noContent().build();
  }

  @GET
  @Path("/projects")
  public List<Projects.ProjectDto> projects() throws URISyntaxException {
    String opalUrl = Optional.ofNullable(studyService.findStudy(id)).map(Study::getOpal).orElse(null);

    return opalService.getProjectDtos(opalUrl);
  }
}
