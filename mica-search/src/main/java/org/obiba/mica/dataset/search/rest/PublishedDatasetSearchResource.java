/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Path("/datasets")
@RequiresAuthentication
@Scope("request")
public class PublishedDatasetSearchResource {

  @Inject
  JoinQueryExecutor joinQueryExecutor;

  @GET
  @Path("/study/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto queryStudy(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("query") String query) throws IOException {

    return queryInternal(from, limit, sort, order, query, StudyDataset.class.getSimpleName(), null);
  }

  @GET
  @Path("/harmonization/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto queryHarmonization(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("query") String query) throws IOException {

    return queryInternal(from, limit, sort, order, query, HarmonizationDataset.class.getSimpleName(), null);
  }

  @GET
  @Path("/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto query(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("study") String studyId, @QueryParam("query") String query)
      throws IOException {

    return queryInternal(from, limit, sort, order, query, null, studyId);
  }

  private MicaSearch.JoinQueryResultDto queryInternal(int from, int limit, String sort, String order, String query,
      String type, String studyId) throws IOException {

    MicaSearch.QueryDto queryDto = QueryDtoHelper
        .createQueryDto(from, limit, sort, order, mergeQueries(createTypeQuery(type), query));
    
    if(!Strings.isNullOrEmpty(studyId)) {
      List<MicaSearch.FilterQueryDto> filters = Lists.newArrayList();
      filters.add(QueryDtoHelper.createTermFilter(DatasetQuery.STUDY_JOIN_FIELD, Arrays.asList(studyId)));
      filters.add(QueryDtoHelper.createTermFilter(DatasetQuery.HARMONIZATION_JOIN_FIELD, Arrays.asList(studyId)));
      queryDto = QueryDtoHelper.addTermFilters(queryDto, filters, QueryDtoHelper.BoolQueryType.SHOULD);
    }

    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.DATASET,
        MicaSearch.JoinQueryDto.newBuilder().setDatasetQueryDto(queryDto).build());
  }

  @POST
  @Path("/_search")
  @Timed
  public MicaSearch.JoinQueryResultDto list(MicaSearch.JoinQueryDto joinQueryDto) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.DATASET, joinQueryDto);
  }

  private static String createTypeQuery(String type) {
    return Strings.isNullOrEmpty(type) ? "" : String.format("(className:%s)", type);
  }

  private static String mergeQueries(String typeQuery, String query) {
    return Strings.isNullOrEmpty(query) ? typeQuery : String.format("%s AND %s", typeQuery, query);
  }

}
