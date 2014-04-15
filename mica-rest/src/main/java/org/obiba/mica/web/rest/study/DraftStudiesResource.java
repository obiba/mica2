package org.obiba.mica.web.rest.study;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.obiba.mica.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/draft")
public class DraftStudiesResource {

  @Inject
  private StudyService studyService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @GET
  @Path("/studies")
  @Timed
  public List<Mica.StudySummaryDto> list() {
    return studyService.findAllStates().stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @Path("/study/{id}")
  public DraftStudyResource study(@PathParam("id") String id) {
    DraftStudyResource studyResource = applicationContext.getBean(DraftStudyResource.class);
    studyResource.setId(id);
    return studyResource;
  }

}
