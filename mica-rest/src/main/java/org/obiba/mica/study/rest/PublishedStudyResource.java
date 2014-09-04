package org.obiba.mica.study.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.study.service.PublishedStudyService;
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
@RequiresAuthentication
public class PublishedStudyResource {

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudyDto get() {
    return dtos.asDto(publishedStudyService.findById(id));
  }

}
