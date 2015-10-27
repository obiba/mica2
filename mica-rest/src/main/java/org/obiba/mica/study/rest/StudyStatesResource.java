package org.obiba.mica.study.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/draft")
public class StudyStatesResource {

  @Inject
  private StudyService studyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/study-states")
  @Timed
  public List<Mica.StudySummaryDto> list() {
    return studyService.findAllStates().stream()
      .filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s.getId()))
      .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(dtos::asDto).collect(Collectors.toList());
  }

  @Path("/study-state/{id}")
  public StudyStateResource study(@PathParam("id") String id) {
    subjectAclService.checkPermission("/draft/study", "VIEW", id);
    StudyStateResource studyStateResource = applicationContext.getBean(StudyStateResource.class);
    studyStateResource.setId(id);
    return studyStateResource;
  }
}
