/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.DebugMethod;
import org.obiba.mica.search.CoverageQueryExecutor;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.GenericReportGenerator;
import org.obiba.mica.search.queries.protobuf.QueryDtoHelper;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;

/**
 * Search for variables in the published variable index.
 */
@Component
@Path("/variables")
@RequiresAuthentication
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
  private GenericReportGenerator genericReportGenerator;

  @GET
  @Timed
  public MicaSearch.JoinQueryResultDto query(@QueryParam("from") @DefaultValue("0") int from,
                                             @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
                                             @QueryParam("order") String order, @QueryParam("query") String query,
                                             @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    return joinQueryExecutor.listQuery(QueryType.VARIABLE,
        QueryDtoHelper.createQueryDto(from, limit, sort, order, query, locale, null, null), locale);
  }

  @GET
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlQuery(@QueryParam("query") String query) throws IOException {
    return joinQueryExecutor.query(QueryType.VARIABLE, searcher.makeJoinQuery(query));
  }

  @GET
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    StreamingOutput stream = os -> genericReportGenerator.generateCsv(QueryType.VARIABLE, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"SearchVariables.csv\"").build();
  }

  @GET
  @DebugMethod
  @Path("/_coverage")
  @Timed
  public MicaSearch.BucketsCoverageDto rqlCoverageAsBucket(@QueryParam("query") String query) throws IOException {
    return coverageByBucketFactory.asBucketsCoverageDto(getTaxonomiesCoverageDto(query, true));
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

  @GET
  @Timed
  @DebugMethod
  @Path("/_coverage_download")
  @Produces("text/csv")
  public Response rqlCoverageDownload(@QueryParam("query") String query) throws IOException {
    return rqlCoverageCsv(query);
  }

  //
  // Private methods
  //

  private MicaSearch.TaxonomiesCoverageDto getTaxonomiesCoverageDto(String query, boolean strict) throws IOException {
    return coverageQueryExecutor.coverageQuery(query, strict);
  }

}
