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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.search.reports.JoinQueryReportGenerator;
import org.obiba.mica.search.reports.ReportGenerator;
import org.obiba.mica.search.reports.generators.SpecificStudyReportGenerator;
import org.obiba.mica.search.reports.generators.StudyCsvReportGenerator;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;

@Component
@Path("/studies")
@Scope("request")
public class PublishedStudiesSearchResource {

  private final MicaConfigService micaConfigService;

  private final JoinQueryExecutor joinQueryExecutor;

  private final Searcher searcher;

  private final JoinQueryReportGenerator joinQueryReportGenerator;

  private final SpecificStudyReportGenerator specificStudyReportGenerator;

  private final PublishedStudyService publishedStudyService;

  protected final PersonService personService;

  @Inject
  public PublishedStudiesSearchResource(MicaConfigService micaConfigService, JoinQueryExecutor joinQueryExecutor, Searcher searcher, JoinQueryReportGenerator joinQueryReportGenerator, SpecificStudyReportGenerator specificStudyReportGenerator, PublishedStudyService publishedStudyService, PersonService personService) {
    this.micaConfigService = micaConfigService;
    this.joinQueryExecutor = joinQueryExecutor;
    this.searcher = searcher;
    this.joinQueryReportGenerator = joinQueryReportGenerator;
    this.specificStudyReportGenerator = specificStudyReportGenerator;
    this.publishedStudyService = publishedStudyService;
    this.personService = personService;
  }

  @GET
  @Timed
  public JoinQueryResultDto rqlList(@QueryParam("from") @DefaultValue("0") int from,
                                    @QueryParam("limit") @DefaultValue("10") int limit,
                                    @QueryParam("sort") @DefaultValue("name") String sort,
                                    @QueryParam("order") @DefaultValue("asc") String order,
                                    @QueryParam("locale") @DefaultValue("en") String locale) {

    String rql = RQLQueryBuilder.newInstance().target(
        RQLQueryBuilder.TargetQueryBuilder.studyInstance().exists("id").limit(from, limit).sort(sort, order).build())
      .locale(locale).buildArgsAsString();

    return joinQueryExecutor.query(QueryType.STUDY, searcher.makeJoinQuery(rql));
  }

  @GET
  @Path("/_rql")
  @Timed
  public JoinQueryResultDto rqlQuery(@QueryParam("query") String query,
                                     @QueryParam("withoutCountStats") @DefaultValue("false") boolean withoutCountStats) {

    String queryStr = query;
    if (Strings.isNullOrEmpty(queryStr)) queryStr = "study(limit(0,0)),network(limit(0,0))";
    return withoutCountStats
      ? joinQueryExecutor.queryWithoutCountStats(QueryType.STUDY, searcher.makeJoinQuery(queryStr))
      : joinQueryExecutor.query(QueryType.STUDY, searcher.makeJoinQuery(queryStr));
  }

  @POST
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlLargeQuery(@FormParam("query") String query,
                                                     @FormParam("withoutCountStats") @DefaultValue("false") boolean withoutCountStats) {
    return rqlQuery(query, withoutCountStats);
  }

  @GET
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    StreamingOutput stream = os -> joinQueryReportGenerator.generateCsv(QueryType.STUDY, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.csv\"").build();
  }

  @POST
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlLargeQueryAsCsv(@FormParam("query") String query, @FormParam("columnsToHide") List<String> columnsToHide) throws IOException {
    return rqlQueryAsCsv(query, columnsToHide);
  }

  @POST
  @Path("/_export")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response export(@FormParam("query") String query, @FormParam("locale") @DefaultValue("en") String locale) {
    if (!micaConfigService.getConfig().isStudiesExportEnabled())
      throw new BadRequestException("Studies export not enabled");
    JoinQuery joinQuery = searcher.makeJoinQuery(query);
    List<String> studyIds = joinQueryExecutor.query(QueryType.STUDY, joinQuery)
      .getStudyResultDto()
      .getExtension(MicaSearch.StudyResultDto.result)
      .getSummariesList()
      .stream()
      .map(Mica.StudySummaryDto::getId)
      .collect(toList());

    ReportGenerator reporter = new StudyCsvReportGenerator(publishedStudyService.findByIds(studyIds, true),
      Strings.isNullOrEmpty(locale) ? joinQuery.getLocale() : locale, personService);
    StreamingOutput stream = reporter::write;
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Studies.zip\"").build();
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
