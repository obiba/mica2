/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search.rest;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.GenericReportGenerator;
import org.obiba.mica.search.csvexport.SpecificStudyReportGenerator;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

import static org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;

@Component
@Path("/studies")
@RequiresAuthentication
@Scope("request")
public class PublishedStudiesSearchResource {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Searcher searcher;

  @Inject
  private GenericReportGenerator genericReportGenerator;

  @Inject
  private SpecificStudyReportGenerator specificStudyReportGenerator;

  @GET
  @Timed
  public JoinQueryResultDto rqlList(@QueryParam("from") @DefaultValue("0") int from,
                                    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") @DefaultValue("name") String sort,
                                    @QueryParam("order") @DefaultValue("asc") String order, @QueryParam("locale") @DefaultValue("en") String locale)
      throws IOException {

    String rql = RQLQueryBuilder.newInstance().target(
        RQLQueryBuilder.TargetQueryBuilder.studyInstance().exists("id").limit(from, limit).sort(sort, order).build())
        .locale(locale).buildArgsAsString();

    return joinQueryExecutor.query(QueryType.STUDY, searcher.makeJoinQuery(rql));
  }

  @GET
  @Path("/_rql")
  @Timed
  public JoinQueryResultDto rqlQuery(@QueryParam("query") String query) throws IOException {
    String queryStr = query;
    if (Strings.isNullOrEmpty(queryStr)) queryStr = "study(exists(Mica_study.id))";
    return joinQueryExecutor.query(QueryType.STUDY, searcher.makeJoinQuery(queryStr));
  }

  @POST
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlLargeQuery(@FormParam("query") String query) throws IOException {
    return rqlQuery(query);
  }

  @GET
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    StreamingOutput stream = os -> genericReportGenerator.generateCsv(QueryType.STUDY, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.csv\"").build();
  }

  @POST
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlLargeQueryAsCsv(@FormParam("query") String query, @FormParam("columnsToHide") List<String> columnsToHide) throws IOException {
    return rqlQueryAsCsv(query, columnsToHide);
  }

  @GET
  @Path("/_report")
  @Produces("text/csv")
  @Timed
  public Response report(@QueryParam("query") String query) throws IOException {
    StreamingOutput stream = os -> specificStudyReportGenerator.report(query, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.csv\"").build();
  }

  @POST
  @Path("/_report")
  @Produces("text/csv")
  @Timed
  public Response reportLargeQuery(@FormParam("query") String query) throws IOException {
    return report(query);
  }

  @GET
  @Path("/_report_by_network")
  @Produces("text/csv")
  @Timed
  public Response report(@QueryParam("networkId") String networkId, @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {
    StreamingOutput stream = os -> specificStudyReportGenerator.report(networkId, locale, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.csv\"").build();
  }

  @POST
  @Path("/_report_by_network")
  @Produces("text/csv")
  @Timed
  public Response reportLargeQuery(@FormParam("networkId") String networkId, @FormParam("locale") @DefaultValue("en") String locale) throws IOException {
    return report(networkId, locale);
  }
}
