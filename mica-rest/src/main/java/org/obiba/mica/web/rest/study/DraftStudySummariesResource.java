package org.obiba.mica.web.rest.study;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.mica.service.study.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/draft")
public class DraftStudySummariesResource {

  @Inject
  private StudyService studyService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/study-summaries")
  @Timed
  public List<Mica.StudySummaryDto> list() {
    return studyService.findAllStates().stream().map(dtos::asDto).collect(Collectors.toList());
  }
  @Path("/study-summary/{id}")
  public DraftStudySummaryResource study(@PathParam("id") String id) {
    DraftStudySummaryResource studyStateResource = applicationContext.getBean(DraftStudySummaryResource.class);
    studyStateResource.setId(id);
    return studyStateResource;
  }
}
