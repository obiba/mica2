/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.search.rest;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.search.reports.JoinQueryReportGenerator;
import org.obiba.mica.search.reports.ReportGenerator;
import org.obiba.mica.search.reports.generators.NetworkCsvReportGenerator;
import org.obiba.mica.spi.search.QueryType;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.obiba.mica.web.model.MicaSearch.JoinQueryResultDto;

@Path("/networks")
@Scope("request")
@Component
public class  PublishedNetworksSearchResource {

  private final MicaConfigService micaConfigService;

  private final JoinQueryExecutor joinQueryExecutor;

  private final Searcher searcher;

  private final JoinQueryReportGenerator joinQueryReportGenerator;

  private final PublishedNetworkService publishedNetworkService;

  protected final PersonService personService;

  @Inject
  public PublishedNetworksSearchResource(MicaConfigService micaConfigService, JoinQueryExecutor joinQueryExecutor, Searcher searcher, JoinQueryReportGenerator joinQueryReportGenerator, PublishedNetworkService publishedNetworkService, PersonService personService) {
    this.micaConfigService = micaConfigService;
    this.joinQueryExecutor = joinQueryExecutor;
    this.searcher = searcher;
    this.joinQueryReportGenerator = joinQueryReportGenerator;
    this.publishedNetworkService = publishedNetworkService;
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
        RQLQueryBuilder.TargetQueryBuilder.networkInstance().exists("id").limit(from, limit).sort(sort, order).build())
        .locale(locale).buildArgsAsString();

    return joinQueryExecutor.query(QueryType.NETWORK, searcher.makeJoinQuery(rql));
  }

  @GET
  @Path("/_rql")
  @Timed
  public JoinQueryResultDto rqlQuery(@QueryParam("query") String query,
                                     @QueryParam("withoutCountStats") @DefaultValue("false") boolean withoutCountStats) {
    String queryStr = query;
    if (Strings.isNullOrEmpty(queryStr)) queryStr = "network(exists(Mica_network.id))";
    return withoutCountStats
      ? joinQueryExecutor.queryWithoutCountStats(QueryType.NETWORK, searcher.makeJoinQuery(queryStr))
      : joinQueryExecutor.query(QueryType.NETWORK, searcher.makeJoinQuery(queryStr));
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
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("studyType") String studyType, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    boolean forHarmonization = !Strings.isNullOrEmpty(studyType) && HarmonizationStudy.RESOURCE_PATH.equals(studyType);
    StreamingOutput stream = os -> joinQueryReportGenerator.generateCsv(QueryType.NETWORK, forHarmonization, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Networks.csv\"").build();
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
  public Response export(@FormParam("query") String query, @FormParam("locale") @DefaultValue("en") String locale) {
    if (!micaConfigService.getConfig().isNetworksExportEnabled())
      throw new BadRequestException("Networks export not enabled");
    JoinQuery joinQuery = searcher.makeJoinQuery(query);
    List<String> networkIds = joinQueryExecutor.query(QueryType.NETWORK, joinQuery)
      .getNetworkResultDto()
      .getExtension(MicaSearch.NetworkResultDto.result)
      .getNetworksList()
      .stream()
      .map(Mica.NetworkDto::getId)
      .collect(toList());

    ReportGenerator reporter = new NetworkCsvReportGenerator(publishedNetworkService.findByIds(networkIds, true),
      Strings.isNullOrEmpty(locale) ? joinQuery.getLocale() : locale, personService);
    StreamingOutput stream = reporter::write;
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"Networks.zip\"").build();
  }
}
