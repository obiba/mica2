/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.obiba.mica.core.DebugMethod;
import org.obiba.mica.search.CoverageQueryExecutor;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.reports.JoinQueryReportGenerator;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

/**
 * Search for variables in the published variable index.
 */
@Component
@Path("/variables")
@Scope("request")
public class PublishedDatasetVariablesSearchResource {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private CoverageQueryExecutor coverageQueryExecutor;

  @Inject
  private Searcher searcher;

  @Inject
  private CoverageByBucketFactory coverageByBucketFactory;

  @Inject
  private JoinQueryReportGenerator joinQueryReportGenerator;

  @GET
  @Timed
  public MicaSearch.JoinQueryResultDto query(@QueryParam("from") @DefaultValue("0") int from,
                                             @QueryParam("limit") @DefaultValue("10") int limit,
                                             @QueryParam("sort") String sort, @QueryParam("order") String order,
                                             @QueryParam("query") String query, @QueryParam("field") List<String> fields,
                                             @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    RQLQueryBuilder.TargetQueryBuilder targetBuilder = RQLQueryBuilder.TargetQueryBuilder.variableInstance()
      .exists("id").limit(from, limit).fields(fields);

    if (!Strings.isNullOrEmpty(query)) {
      targetBuilder.match(query);
    }

    if (!Strings.isNullOrEmpty(sort)) {
      targetBuilder.sort(sort, order);
    }

    return rqlQuery(RQLQueryBuilder.newInstance().target(targetBuilder.build()).locale(locale).buildArgsAsString(), true);
  }

  @GET
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlQuery(@QueryParam("query") String query,
                                                @QueryParam("withoutCountStats") @DefaultValue("false") boolean withoutCountStats) {
    return withoutCountStats
      ? joinQueryExecutor.queryWithoutCountStats(QueryType.VARIABLE, searcher.makeJoinQuery(query))
      : joinQueryExecutor.query(QueryType.VARIABLE, searcher.makeJoinQuery(query));
  }

  @POST
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlLargeQuery(@FormParam("query") String query,
                                                     @QueryParam("withoutCountStats") @DefaultValue("false") boolean withoutCountStats) {
    return rqlQuery(query, withoutCountStats);
  }

  @GET
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("studyType") String studyType, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    boolean forHarmonization = !Strings.isNullOrEmpty(studyType) && HarmonizationStudy.RESOURCE_PATH.equals(studyType);
    StreamingOutput stream = os -> joinQueryReportGenerator.generateCsv(QueryType.VARIABLE, forHarmonization, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Variables.csv\"").build();
  }

  @POST
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlLargeQueryAsCsv(@FormParam("query") String query, @FormParam("studyType") String studyType, @FormParam("columnsToHide") List<String> columnsToHide) throws IOException {
    return rqlQueryAsCsv(query, studyType, columnsToHide);
  }

  @GET
  @DebugMethod
  @Path("/_coverage")
  @Timed
  public MicaSearch.BucketsCoverageDto rqlCoverageAsBucket(@QueryParam("query") String query, @QueryParam("withZeros") @DefaultValue("true") boolean withZeros) throws IOException {
    return coverageByBucketFactory.asBucketsCoverageDto(getTaxonomiesCoverageDto(query, true), withZeros);
  }

  @POST
  @DebugMethod
  @Path("/_coverage")
  @Timed
  public MicaSearch.BucketsCoverageDto largeRqlCoverageAsBucket(@FormParam("query") String query, @FormParam("withZeros") @DefaultValue("true") boolean withZeros) throws IOException {
    return rqlCoverageAsBucket(query, withZeros);
  }

  @GET
  @DebugMethod
  @Path("/legacy/_coverage")
  @Timed
  public MicaSearch.TaxonomiesCoverageDto rqlCoverageAsDto(@QueryParam("query") String query,
                                                           @QueryParam("strict") @DefaultValue("true") boolean strict) throws IOException {
    return getTaxonomiesCoverageDto(query, strict);
  }

  @GET
  @DebugMethod
  @Path("/charts/_coverage")
  @Timed
  public MicaSearch.ChartsCoverageDto rqlCoverageForChartData(@QueryParam("query") String query,
                                                              @QueryParam("includeTerms") @DefaultValue("false") boolean includeTerms) throws IOException {
    return (new CoverageChartDataFactory().makeChartData(getTaxonomiesCoverageDto(query, false), includeTerms));
  }

  @POST
  @DebugMethod
  @Path("/charts/_coverage")
  @Timed
  public MicaSearch.ChartsCoverageDto largeRqlCoverageForChartData(@FormParam("query") String query,
                                                                   @QueryParam("includeTerms") @DefaultValue("false") boolean includeTerms) throws IOException {
    return rqlCoverageForChartData(query, includeTerms);
  }

  @GET
  @Timed
  @DebugMethod
  @Path("/_coverage")
  @Produces("text/csv")
  public Response rqlCoverageCsv(@QueryParam("query") String query) throws IOException {
    CsvCoverageWriter writer = new CsvCoverageWriter();
    return Response
        .ok(writer.write(coverageByBucketFactory.makeCoverageByBucket(getTaxonomiesCoverageDto(query, true))).toByteArray(), "text/csv")
        .header("Content-Disposition", "attachment; filename=\"coverage.csv\"").build();
  }

  @POST
  @Timed
  @DebugMethod
  @Path("/_coverage")
  @Produces("text/csv")
  public Response largeRqlCoverageCsv(@FormParam("query") String query) throws IOException {
    return rqlCoverageCsv(query);
  }

  @GET
  @Timed
  @DebugMethod
  @Path("/_coverage_download")
  @Produces("text/csv")
  public Response rqlCoverageDownload(@QueryParam("query") String query) throws IOException {
    return rqlCoverageCsv(query);
  }

  @POST
  @Timed
  @DebugMethod
  @Path("/_coverage_download")
  @Produces("text/csv")
  public Response largeRqlCoverageDownload(@FormParam("query") String query) throws IOException {
    return rqlCoverageDownload(query);
  }

  //
  // Private methods
  //

  private MicaSearch.TaxonomiesCoverageDto getTaxonomiesCoverageDto(String query, boolean strict) throws IOException {
    return coverageQueryExecutor.coverageQuery(query, strict);
  }

}
