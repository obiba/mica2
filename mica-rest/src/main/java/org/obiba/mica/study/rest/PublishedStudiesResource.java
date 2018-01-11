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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/studies")
@Scope("request")
@RequiresAuthentication
public class PublishedStudiesResource {

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private Dtos dtos;

  @GET
  @Path("/_list")
  @Timed
  public Mica.StudySummariesDto list(@QueryParam("from") @DefaultValue("0") int from,
                                     @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
                                     @QueryParam("order") String order, @QueryParam("query") String query) {

    PublishedDocumentService.Documents<BaseStudy> studies = publishedStudyService.find(from, limit, sort, order, null, query);

    Mica.StudySummariesDto.Builder builder = Mica.StudySummariesDto.newBuilder();

    builder.setFrom(studies.getFrom()).setLimit(studies.getLimit()).setTotal(studies.getTotal());
    builder.addAllStudySummaries(studies.getList().stream().map(baseStudy -> dtos.asSummaryDto(baseStudy)).collect(Collectors.toList()));

    return builder.build();
  }

  @GET
  @Path("_suggest")
  @Timed
  public List<String> suggest(@QueryParam("locale") @DefaultValue("en") String locale, @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("query") String query) {
    if (Strings.isNullOrEmpty(query)) return Lists.newArrayList();
    return publishedStudyService.suggest(limit, locale, query);
  }
}
