/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.reports.JoinQueryReportGenerator;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

@Component
@Path("/datasets")
@Scope("request")
public class PublishedDatasetSearchResource {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Searcher searcher;

  @Inject
  private JoinQueryReportGenerator joinQueryReportGenerator;

  @GET
  @Timed
  public MicaSearch.JoinQueryResultDto rqlList(@QueryParam("from") @DefaultValue("0") int from,
                                               @QueryParam("limit") @DefaultValue("10") int limit,
                                               @QueryParam("sort") @DefaultValue("name") String sort,
                                               @QueryParam("order") String order,
                                               @QueryParam("locale") @DefaultValue("en") String locale) {

    String rql = RQLQueryBuilder.newInstance().target(
        RQLQueryBuilder.TargetQueryBuilder.datasetInstance().exists("id").limit(from, limit).sort(sort, order).build())
        .locale(locale).buildArgsAsString();

    return joinQueryExecutor.query(QueryType.DATASET, searcher.makeJoinQuery(rql));
  }

  @GET
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlQuery(@QueryParam("query") String query,
                                                @QueryParam("withoutCountStats") @DefaultValue("false") boolean withoutCountStats) {
    String queryStr = query;
    if (Strings.isNullOrEmpty(queryStr)) queryStr = "dataset(exists(Mica_dataset.id))";
    return withoutCountStats
      ? joinQueryExecutor.queryWithoutCountStats(QueryType.DATASET, searcher.makeJoinQuery(queryStr))
      : joinQueryExecutor.query(QueryType.DATASET, searcher.makeJoinQuery(queryStr));
  }

  @POST
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlLargeQuery(@FormParam("query") String query,
                                                     @FormParam("withoutCountStats") @DefaultValue("false")  boolean withoutCountStats) {
    return rqlQuery(query, withoutCountStats);
  }

  @GET
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    StreamingOutput stream = os -> joinQueryReportGenerator.generateCsv(QueryType.DATASET, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Datasets.csv\"").build();
  }

  @POST
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlLargeQueryAsCsv(@FormParam("query") String query, @FormParam("columnsToHide") List<String> columnsToHide) throws IOException {
    return rqlQueryAsCsv(query, columnsToHide);
  }

}
