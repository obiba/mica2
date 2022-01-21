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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.EntityStateFilter;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.search.AccessibleIdFilterBuilder;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;
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

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
  public List<Mica.StudySummaryDto> listCollectionStudyStates(@QueryParam("query") String query,
                                                              @QueryParam("from") @DefaultValue("0") Integer from,
                                                              @QueryParam("limit") Integer limit,
                                                              @QueryParam("sort") @DefaultValue("id") String sort,
                                                              @QueryParam("order") @DefaultValue("asc") String order,
                                                              @QueryParam("type") String type,
                                                              @QueryParam("exclude") List<String> excludes,
                                                              @QueryParam("filter") @DefaultValue("ALL") String filter,
                                                              @Context HttpServletResponse response) {
    Stream<? extends EntityState> result;
    long totalCount;

    EntityStateFilter entityStateFilter = EntityStateFilter.valueOf(filter);
    List<String> filteredIds = Strings.isNullOrEmpty(type)
      ? studyService.getIdsByStateFilter(entityStateFilter)
      : getStudyServiceByType(type).getIdsByStateFilter(entityStateFilter);

    Searcher.IdFilter accessibleIdFilter = AccessibleIdFilterBuilder.newBuilder()
      .aclService(subjectAclService)
      .resources(getPermissionResources(type))
      .ids(filteredIds)
      .build();


    String ids = excludes.stream().map(s -> "id:" + s).collect(Collectors.joining(" "));

    if(!Strings.isNullOrEmpty(ids)) {
      if (Strings.isNullOrEmpty(query)) query = String.format("NOT(%s)", ids);
      else query += String.format(" AND NOT(%s)", ids);
    }

    if(limit == null) limit = MAX_LIMIT;
    if(limit < 0) throw new IllegalArgumentException("limit cannot be negative");

    DocumentService.Documents<BaseStudy> studyDocuments = draftStudyService.find(from, limit, sort, order,
      null, query, null, null, accessibleIdFilter);

    totalCount = studyDocuments.getTotal();
    response.addHeader("X-Total-Count", Long.toString(totalCount));

    return studyDocuments.getList()
      .stream()
      .map(study -> dtos.asDto(study, studyService.getEntityState(study.getId())))
      .collect(toList());
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

  private List<String> getPermissionResources(@NotNull String type) {
    if (Strings.isNullOrEmpty(type)) {
      return Lists.newArrayList("/draft/individual-study", "/draft/harmonization-study");
    }

    return "individual-study".equals(type)
      ? Lists.newArrayList("/draft/individual-study")
      : Lists.newArrayList("/draft/harmonization-study");
  }
}
