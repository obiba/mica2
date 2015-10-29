package org.obiba.mica.study.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.security.service.SubjectAclService;
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
  private SubjectAclService subjectAclService;

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
  public Mica.StudySummaryDto get() {
    subjectAclService.checkPermission("/draft/study", "VIEW", id);
    return dtos.asDto(studyService.getEntityState(id));
  }

  @GET
  @Path("/projects")
  public List<Projects.ProjectDto> projects() throws URISyntaxException {
    subjectAclService.checkPermission("/draft/study", "VIEW", id);
    String opalUrl = Optional.ofNullable(studyService.findStudy(id)).map(Study::getOpal).orElse(null);

    return opalService.getProjectDtos(opalUrl);
  }
}
