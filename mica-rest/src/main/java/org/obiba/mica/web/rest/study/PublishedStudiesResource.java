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

@Path("/")
public class PublishedStudiesResource {

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
    return studyService.findPublishedStates().stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @Path("/study/{id}")
  public PublishedStudyResource study(@PathParam("id") String id) {
    PublishedStudyResource studyResource = applicationContext.getBean(PublishedStudyResource.class);
    studyResource.setId(id);
    return studyResource;
  }

}
