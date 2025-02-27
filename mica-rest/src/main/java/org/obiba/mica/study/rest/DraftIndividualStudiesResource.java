/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Path("/draft")
@Scope("request")
public class DraftIndividualStudiesResource {

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private EventBus eventBus;

  @GET
  @Path("/individual-studies")
  @Timed
  public List<Mica.StudyDto> list() {
    return individualStudyService.findAllDraftStudies().stream()
      .filter(s -> subjectAclService.isPermitted("/draft/individual-study", "VIEW", s.getId()))
      .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(s -> dtos.asDto(s, true)).collect(Collectors.toList());
  }

  @GET
  @Path("/individual-studies/summaries")
  @Timed
  public List<Mica.StudySummaryDto> listSummaries(@QueryParam("id") List<String> ids) {
    List<Study> studies = ids.isEmpty() ? individualStudyService.findAllDraftStudies() : individualStudyService.findAllDraftStudies(ids);
    return studies.stream().filter(s -> subjectAclService.isPermitted("/draft/individual-study", "VIEW", s.getId()))
      .map(dtos::asSummaryDto).collect(Collectors.toList());
  }

  @GET
  @Path("/individual-studies/digests")
  @Timed
  public List<Mica.DocumentDigestDto> listDigests(@QueryParam("id") List<String> ids) {
    List<Study> studies = ids.isEmpty() ? individualStudyService.findAllDraftStudies() : individualStudyService.findAllDraftStudies(ids);
    return studies.stream().filter(s -> subjectAclService.isPermitted("/draft/individual-study", "VIEW", s.getId())).map(dtos::asDigestDto).collect(Collectors.toList());
  }

  @POST
  @Path("/individual-studies")
  @Timed
  @RequiresPermissions({"/draft/individual-study:ADD"})
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Context UriInfo uriInfo,
    @Nullable @QueryParam("comment") String comment) {
    Study study = (Study)dtos.fromDto(studyDto);
    individualStudyService.save(study, comment);
    return Response.created(uriInfo.getBaseUriBuilder().path(DraftIndividualStudiesResource.class, "study").build(study.getId()))
      .build();
  }

  @Path("/individual-study/{id}")
  public DraftIndividualStudyResource study(@PathParam("id") String id) {
    DraftIndividualStudyResource studyResource = applicationContext.getBean(DraftIndividualStudyResource.class);
    studyResource.setId(id);
    return studyResource;
  }

  @PUT
  @Path("/individual-studies/_index")
  @Timed
  @RequiresPermissions("/draft/individual-study:PUBLISH")
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    eventBus.post(new IndexStudiesEvent(ids));
    return Response.noContent().build();
  }
}
