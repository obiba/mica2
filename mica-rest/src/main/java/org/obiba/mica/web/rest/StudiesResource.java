package org.obiba.mica.web.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
   * POST  /rest/studies -> Create a new study.
   */
  @POST
  @Timed
  public void create(Mica.StudyDtoOrBuilder studyDto) {
    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
  }

  /**
   * GET  /rest/studies -> get all the studies.
   */
  @GET
  @Timed
  public List<Study> getAll() {
    return studyService.findAll();
  }

  /**
   * GET  /rest/studies/:id -> get the "id" study.
   */
  @GET
  @Path("/{id}")
  @Timed
  public Study get(@PathParam("id") String id) {
    return studyService.findById(id);
  }

  /**
   * DELETE  /rest/studies/:id -> delete the "id" study.
   */
  @DELETE
  @Path("/{id}")
  @Timed
  public void delete(@PathParam("id") String id) {
    studyService.delete(id);
  }
}
