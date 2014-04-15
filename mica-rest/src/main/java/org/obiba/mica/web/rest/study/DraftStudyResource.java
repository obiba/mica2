package org.obiba.mica.web.rest.study;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.obiba.mica.domain.Study;
import org.obiba.mica.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing draft Study.
 */
@Component
@Scope("request")
public class DraftStudyResource {

  @Inject
  private StudyService studyService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudyDto get() {
    return dtos.asDto(studyService.findDraftStudy(id));
  }

  @POST
  @Timed
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Context UriInfo uriInfo) {
    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
    return Response.created(uriInfo.getBaseUriBuilder().path(DraftStudyResource.class).build(study.getId())).build();
  }

  @PUT
  @Timed
  public Response update(Mica.StudyDtoOrBuilder studyDto) {
    // ensure study exists
    studyService.findDraftStudy(id);

    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
    return Response.noContent().build();
  }

  /**
   * DELETE  /ws/studies/:id -> delete the "id" study.
   */
//  @DELETE
//  @Timed
//  public Response delete() {
//    studyService.delete(id);
//    return Response.noContent().build();
//  }

}
