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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.AbstractStudyService;
import org.obiba.mica.study.service.DraftStudyService;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

@Path("/draft")
public class StudyStatesResource {

  private static final int MAX_LIMIT = 10000; //default ElasticSearch limit

  @Inject
  private StudyService studyService;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private DraftStudyService draftStudyService;

  @GET
  @Path("/study-states")
  @Timed
  public List<Mica.StudySummaryDto> listCollectionStudyStates(@QueryParam("query") String query, @QueryParam("from") @DefaultValue("0") Integer from,
                                         @QueryParam("limit") Integer limit, @QueryParam("type") String type, @Context HttpServletResponse response) {
    Stream<? extends EntityState> result;
    long totalCount;

    if(limit == null) limit = MAX_LIMIT;

    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    if(Strings.isNullOrEmpty(query)) {
      List<? extends EntityState> studyStates =
        (!Strings.isNullOrEmpty(type) ? getStudyServiceByType(type).findAllStates() : studyService.findAllStates()).stream()
        .filter(s -> subjectAclService.isPermitted("/draft/individual-study", "VIEW", s.getId())
          || subjectAclService.isPermitted("/draft/harmonization-study", "VIEW", s.getId()))
          .collect(toList());
      totalCount = studyStates.size();
      result = studyStates.stream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).skip(from).limit(limit);
    } else {
      DocumentService.Documents<Study> studyDocuments = draftStudyService.find(from, limit, null, null, null, query);
      totalCount = studyDocuments.getTotal();
      result = (!Strings.isNullOrEmpty(type)
        ? getStudyServiceByType(type).findAllStates(studyDocuments.getList().stream().map(Study::getId).collect(toList()))
        : studyService.findAllStates(studyDocuments.getList().stream().map(Study::getId).collect(toList())))
        .stream();
    }

    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return result.map(dtos::asDto).collect(toList());
  }

  @Path("/study-state/{id}")
  public StudyStateResource study(@PathParam("id") String id) {
    StudyStateResource studyStateResource = applicationContext.getBean(StudyStateResource.class);
    studyStateResource.setId(id);
    return studyStateResource;
  }

  private AbstractStudyService<? extends EntityState, ? extends BaseStudy> getStudyServiceByType(@NotNull String type) {
    return "individual-study".equals(type) ? individualStudyService : harmonizationStudyService;
  }
}
