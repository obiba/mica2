package org.obiba.mica.web.rest.study;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.obiba.mica.domain.Study;
import org.obiba.mica.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Study.
 */
@Component
@Path("/studies")
public class StudiesResource {

  @Inject
  private StudyService studyService;

  @Inject
  private Dtos dtos;

  /**
   * POST  /ws/studies -> Create a new study.
   */
  @POST
  @Timed
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Context UriInfo uriInfo) {
    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
    return Response.created(uriInfo.getBaseUriBuilder().path(StudyResource.class, "get").build(study.getId())).build();
  }

  /**
   * GET  /ws/studies -> get all the studies.
   */
  @GET
  @Timed
  public List<Mica.StudyDto> list() {
    return studyService.findAll().stream().map(dtos::asDto).collect(Collectors.toList());
  }

}
