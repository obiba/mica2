package org.obiba.mica.web.rest.study;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.obiba.mica.domain.Study;
import org.obiba.mica.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Study.
 */
@Component
@Scope("request")
@Path("/studies/{id}")
public class StudyResource {

  @Inject
  private StudyService studyService;

  @Inject
  private Dtos dtos;

  @PathParam("id")
  private String id;

  /**
   * GET  /ws/studies/:id -> get the "id" study.
   */
  @GET
  @Timed
  public Mica.StudyDto get() {
    return dtos.asDto(studyService.findById(id));
  }

  @PUT
  @Timed
  public Response update(Mica.StudyDtoOrBuilder studyDto) {
    // ensure study exists
    studyService.findById(id);

    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
    return Response.noContent().build();
  }

  /**
   * DELETE  /ws/studies/:id -> delete the "id" study.
   */
  @DELETE
  @Timed
  public Response delete() {
    studyService.delete(id);
    return Response.noContent().build();
  }
}
