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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
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
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
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
  public Response rqlQueryAsCsv(@QueryParam("query") String query,@QueryParam("studyType") String studyType, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    boolean forHarmonization = !Strings.isNullOrEmpty(studyType) && HarmonizationStudy.RESOURCE_PATH.equals(studyType);
    String fileName = forHarmonization ? "Initiatives" : "Studies";
    StreamingOutput stream = os -> joinQueryReportGenerator.generateCsv(QueryType.STUDY, forHarmonization, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\""+ fileName + ".csv\"").build();
  }

  @POST
  @Path("/_rql_csv")
  @Produces("text/csv")
  @Timed
  public Response rqlLargeQueryAsCsv(@FormParam("query") String query, @FormParam("studyType") String studyType, @FormParam("columnsToHide") List<String> columnsToHide) throws IOException {
    return rqlQueryAsCsv(query, studyType, columnsToHide);
  }

  @POST
  @Path("/_export")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response export(@FormParam("query") String query, @FormParam("locale") @DefaultValue("en") String locale, @FormParam("studyType") String studyType) {
    if (!micaConfigService.getConfig().isStudiesExportEnabled())
      throw new BadRequestException("Studies export not enabled");
    boolean forHarmonization = !Strings.isNullOrEmpty(studyType) && HarmonizationStudy.RESOURCE_PATH.equals(studyType);
    String fileName = forHarmonization ? "Initiatives" : "Studies";
    JoinQuery joinQuery = searcher.makeJoinQuery(query);
    List<String> studyIds = joinQueryExecutor.query(QueryType.STUDY, joinQuery)
      .getStudyResultDto()
      .getExtension(MicaSearch.StudyResultDto.result)
      .getSummariesList()
      .stream()
      .map(Mica.StudySummaryDto::getId)
      .collect(toList());

    ReportGenerator reporter = new StudyCsvReportGenerator(publishedStudyService.findByIds(studyIds, true),
      Strings.isNullOrEmpty(locale) ? joinQuery.getLocale() : locale, personService, forHarmonization);
    StreamingOutput stream = reporter::write;
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\""+ fileName + ".zip\"").build();
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
  public Response report(@QueryParam("networkId") String networkId, @QueryParam("locale") @DefaultValue("en") String locale, @QueryParam("studyType") String studyType) throws IOException {
    String fileName = "StudiesInitiatives.csv";
    String className = null;

    if (Study.RESOURCE_PATH.equals(studyType)) {
      fileName = "Studies.csv";
      className = Study.class.getSimpleName();
    } else if (HarmonizationStudy.RESOURCE_PATH.equals(studyType)) {
      fileName = "Initiatives.csv";
      className = HarmonizationStudy.class.getSimpleName();
    }

    String finalClassName = className;
    StreamingOutput stream = os -> specificStudyReportGenerator.report(networkId, locale, os, finalClassName);
    return Response.ok(stream).header("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName)).build();
  }

  @POST
  @Path("/_report_by_network")
  @Produces("text/csv")
  @Timed
  public Response reportLargeQuery(@FormParam("networkId") String networkId, @FormParam("locale") @DefaultValue("en") String locale, @FormParam("studyType") String studyType) throws IOException {
    return report(networkId, locale, studyType);
  }
}
