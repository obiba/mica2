package org.obiba.mica.study.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.core.security.Roles;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/draft")
@RequiresAuthentication
@RequiresRoles(Roles.MICA_ADMIN)
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
  public List<Mica.StudyDto> list() {
    return studyService.findAllDraftStudies().stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @GET
  @Path("/studies/summaries")
  @Timed
  public List<Mica.StudySummaryDto> listSummaries(@QueryParam("id") List<String> ids) {
    List<Study> studies = ids.isEmpty() ? studyService.findAllDraftStudies() : studyService.findAllDraftStudies(ids);
    return studies.stream().map(dtos::asSummaryDto).collect(Collectors.toList());
  }

  @GET
  @Path("/studies/digests")
  @Timed
  public List<Mica.DocumentDigestDto> listDigests(@QueryParam("id") List<String> ids) {
    List<Study> studies = ids.isEmpty() ? studyService.findAllDraftStudies() : studyService.findAllDraftStudies(ids);
    return studies.stream().map(dtos::asDigestDto).collect(Collectors.toList());
  }

  @POST
  @Path("/studies")
  @Timed
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Context UriInfo uriInfo) {
    Study study = dtos.fromDto(studyDto);
    studyService.save(study);
    return Response.created(uriInfo.getBaseUriBuilder().path(DraftStudiesResource.class, "study").build(study.getId()))
        .build();
  }

  @Path("/study/{id}")
  public DraftStudyResource study(@PathParam("id") String id) {
    DraftStudyResource studyResource = applicationContext.getBean(DraftStudyResource.class);
    studyResource.setId(id);
    return studyResource;
  }

}
