/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.csvexport.GenericReportGenerator;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.search.queries.protobuf.JoinQueryDtoWrapper;
import org.obiba.mica.search.queries.protobuf.QueryDtoHelper;
import org.obiba.mica.search.queries.rql.RQLQueryBuilder;
import org.obiba.mica.search.queries.rql.RQLQueryFactory;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Component
@Path("/datasets")
@RequiresAuthentication
@Scope("request")
public class PublishedDatasetSearchResource {

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private RQLQueryFactory rqlQueryFactory;

  @Inject
  private GenericReportGenerator genericReportGenerator;

  @GET
  @Path("/study/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto listStudy(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") String order, @QueryParam("query") String query,
    @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    return listInternal(from, limit, sort, order, query, locale, StudyDataset.class.getSimpleName(), null);
  }

  @GET
  @Path("/harmonization/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto listHarmonization(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") String order, @QueryParam("query") String query,
    @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    return listInternal(from, limit, sort, order, query, locale, HarmonizationDataset.class.getSimpleName(), null);
  }

  @GET
  @Timed
  public MicaSearch.JoinQueryResultDto rqlList(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") @DefaultValue("name") String sort,
    @QueryParam("order") String order, @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    String rql = RQLQueryBuilder.newInstance().target(
      RQLQueryBuilder.TargetQueryBuilder.datasetInstance().exists("id").limit(from, limit).sort(sort, order).build())
      .locale(locale).buildArgsAsString();

    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.DATASET, rqlQueryFactory.makeJoinQuery(rql));
  }

  @GET
  @Path("/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
    @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
    @QueryParam("order") String order, @QueryParam("study") String studyId, @QueryParam("query") String query,
    @QueryParam("locale") @DefaultValue("en") String locale) throws IOException {

    return listInternal(from, limit, sort, order, query, locale, null, studyId);
  }

  private MicaSearch.JoinQueryResultDto listInternal(int from, int limit, String sort, String order, String query,
    String locale, String type, String studyId) throws IOException {

    MicaSearch.QueryDto queryDto = QueryDtoHelper
      .createQueryDto(from, limit, sort, order, mergeQueries(createTypeQuery(type), query), locale,
        Stream.of(DatasetIndexer.LOCALIZED_ANALYZED_FIELDS));

    if(!Strings.isNullOrEmpty(studyId)) {
      List<MicaSearch.FieldFilterQueryDto> filters = Lists.newArrayList();

      if(type != null) {
        if(type.equals(StudyDataset.class.getSimpleName())) {
          filters.add(QueryDtoHelper.createTermFilter(DatasetQuery.STUDY_JOIN_FIELD, Arrays.asList(studyId)));
        } else {
          filters.add(QueryDtoHelper.createTermFilter(DatasetQuery.HARMONIZATION_JOIN_FIELD, Arrays.asList(studyId)));
        }
        queryDto = QueryDtoHelper.addTermFilters(queryDto, filters, QueryDtoHelper.BoolQueryType.MUST);
      } else {
        filters.add(QueryDtoHelper.createTermFilter(DatasetQuery.STUDY_JOIN_FIELD, Arrays.asList(studyId)));
        filters.add(QueryDtoHelper.createTermFilter(DatasetQuery.HARMONIZATION_JOIN_FIELD, Arrays.asList(studyId)));
        queryDto = QueryDtoHelper.addTermFilters(queryDto, filters, QueryDtoHelper.BoolQueryType.SHOULD);
      }
    }

    MicaSearch.JoinQueryResultDto.Builder builder = joinQueryExecutor
      .listQuery(JoinQueryExecutor.QueryType.DATASET, queryDto, locale).toBuilder();
    builder.clearNetworkResultDto().clearVariableResultDto().clearStudyResultDto();
    builder.setDatasetResultDto(builder.getDatasetResultDto().toBuilder().clearAggs().build());
    return builder.build();
  }

  @POST
  @Path("/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto query(MicaSearch.JoinQueryDto joinQueryDto) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.DATASET, new JoinQueryDtoWrapper(joinQueryDto));
  }

  @GET
  @Path("/_rql")
  @Timed
  public MicaSearch.JoinQueryResultDto rqlQuery(@QueryParam("query") String query) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.DATASET, rqlQueryFactory.makeJoinQuery(query));
  }

  @GET
  @Path("/_rql_csv")
  @Timed
  public Response rqlQueryAsCsv(@QueryParam("query") String query, @QueryParam("columnsToHide") List<String> columnsToHide) throws IOException {
    StreamingOutput stream = os -> genericReportGenerator.generateCsv(JoinQueryExecutor.QueryType.DATASET, query, columnsToHide, os);
    return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"SearchDatasets.csv\"").build();
  }

  private static String createTypeQuery(String type) {
    return Strings.isNullOrEmpty(type) ? "" : String.format("(className:%s)", type);
  }

  private static String mergeQueries(String typeQuery, String query) {
    return Strings.isNullOrEmpty(typeQuery)
      ? query
      : Strings.isNullOrEmpty(query) ? typeQuery : String.format("%s AND %s", typeQuery, query);
  }
}
