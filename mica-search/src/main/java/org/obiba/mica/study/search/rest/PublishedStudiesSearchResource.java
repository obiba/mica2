/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.rest.QueryDtoBuilders;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

import static org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;

@Path("/studies/_search")
@RequiresAuthentication
@Scope("request")
public class PublishedStudiesSearchResource {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @GET
  @Timed
  public JoinQueryResultDto query(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("query") String query) throws IOException {

    QueryDtoBuilders.ExtendedQueryDtoBuilder builder = QueryDtoBuilders.ExtendedQueryDtoBuilder.newBuilder().from(from)
        .size(limit);

    if (!Strings.isNullOrEmpty(query)) {
      builder.queryString(query);
    }

    if (!Strings.isNullOrEmpty(sort)) {
      builder.sort(sort, order == null ? SortOrder.ASC.name() : order);
    }

    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.STUDY,
        MicaSearch.JoinQueryDto.newBuilder().setStudyQueryDto(builder.build()).build());
  }

  @POST
  @Timed
  public JoinQueryResultDto list(MicaSearch.JoinQueryDto joinQueryDto) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.STUDY, joinQueryDto);
  }
}
