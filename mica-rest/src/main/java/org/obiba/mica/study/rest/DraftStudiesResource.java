/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.service.CollectionStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;

@Path("/draft")
public class DraftStudiesResource {

  @Inject
  private CollectionStudyService collectionStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private EventBus eventBus;

  @GET
  @Path("/studies")
  @Timed
  public List<Mica.StudyDto> list() {
    return collectionStudyService.findAllDraftStudies().stream()
      .filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s.getId()))
      .sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(s -> dtos.asDto(s, true)).collect(Collectors.toList());
  }

  @GET
  @Path("/studies/summaries")
  @Timed
  public List<Mica.StudySummaryDto> listSummaries(@QueryParam("id") List<String> ids) {
    List<Study> studies = ids.isEmpty() ? collectionStudyService.findAllDraftStudies() : collectionStudyService.findAllDraftStudies(ids);
    return studies.stream().filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s.getId()))
      .map(dtos::asSummaryDto).collect(Collectors.toList());
  }

  @GET
  @Path("/studies/digests")
  @Timed
  public List<Mica.DocumentDigestDto> listDigests(@QueryParam("id") List<String> ids) {
    List<Study> studies = ids.isEmpty() ? collectionStudyService.findAllDraftStudies() : collectionStudyService.findAllDraftStudies(ids);
    return studies.stream().filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s.getId())).map(dtos::asDigestDto).collect(Collectors.toList());
  }

  @POST
  @Path("/studies")
  @Timed
  @RequiresPermissions({"/draft/study:ADD"})
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Context UriInfo uriInfo,
    @Nullable @QueryParam("comment") String comment) {
    Study study = dtos.fromDto(studyDto);
    collectionStudyService.save(study, comment);
    return Response.created(uriInfo.getBaseUriBuilder().path(DraftStudiesResource.class, "study").build(study.getId()))
      .build();
  }

  @Path("/study/{id}")
  public DraftStudyResource study(@PathParam("id") String id) {
    DraftStudyResource studyResource = applicationContext.getBean(DraftStudyResource.class);
    studyResource.setId(id);
    return studyResource;
  }

  @PUT
  @Path("/studies/_index")
  @Timed
  @RequiresPermissions("/draft/study:PUBLISH")
  public Response reIndex() {
    eventBus.post(new IndexStudiesEvent());
    return Response.noContent().build();
  }
}
