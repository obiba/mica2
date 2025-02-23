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

import java.util.Comparator;
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

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Path("/draft")
@Scope("request")
public class DraftHarmonizationStudiesResource {

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private EventBus eventBus;

  @GET
  @Path("/harmonization-studies")
  @Timed
  public List<Mica.StudyDto> list() {
    return harmonizationStudyService.findAllDraftStudies().stream()
      .filter(s -> subjectAclService.isPermitted("/draft/harmonization-study", "VIEW", s.getId()))
      .sorted(Comparator.comparing(AbstractGitPersistable::getId)).map(s -> dtos.asDto(s, true)).collect(Collectors.toList());
  }

  @GET
  @Path("/harmonization-studies/summaries")
  @Timed
  public List<Mica.StudySummaryDto> listSummaries(@QueryParam("id") List<String> ids) {
    List<HarmonizationStudy> studies = ids.isEmpty() ? harmonizationStudyService.findAllDraftStudies() : harmonizationStudyService.findAllDraftStudies(ids);
    return studies.stream().filter(s -> subjectAclService.isPermitted("/draft/harmonization-study", "VIEW", s.getId()))
      .map(dtos::asSummaryDto).collect(Collectors.toList());
  }

  @POST
  @Path("/harmonization-studies")
  @Timed
  @RequiresPermissions({"/draft/harmonization-study:ADD"})
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.StudyDto studyDto, @Context UriInfo uriInfo,
    @Nullable @QueryParam("comment") String comment) {
    HarmonizationStudy study = (HarmonizationStudy)dtos.fromDto(studyDto);
    harmonizationStudyService.save(study, comment);
    return Response.created(uriInfo.getBaseUriBuilder().path(DraftHarmonizationStudiesResource.class, "study").build(study.getId()))
      .build();
  }

  @Path("/harmonization-study/{id}")
  public DraftHarmonizationStudyResource study(@PathParam("id") String id) {
    DraftHarmonizationStudyResource studyResource = applicationContext.getBean(DraftHarmonizationStudyResource.class);
    studyResource.setId(id);
    return studyResource;
  }

  @PUT
  @Path("/harmonization-studies/_index")
  @Timed
  @RequiresPermissions("/draft/harmonization-study:PUBLISH")
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    // TODO: reindex only the harmonization studies
    eventBus.post(new IndexStudiesEvent(ids));
    return Response.noContent().build();
  }
}
