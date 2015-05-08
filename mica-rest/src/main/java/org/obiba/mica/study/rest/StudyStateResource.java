package org.obiba.mica.study.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
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

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  @RequiresPermissions({"mica:/draft:EDIT"})
  public Mica.StudySummaryDto get() {
    return dtos.asDto(studyService.findStateById(id));
  }

  @PUT
  @Path("/_publish")
  @RequiresPermissions({"mica:/draft:PUBLISH"})
  public Response publish() {
    studyService.publish(id);
    return Response.noContent().build();
  }

}
