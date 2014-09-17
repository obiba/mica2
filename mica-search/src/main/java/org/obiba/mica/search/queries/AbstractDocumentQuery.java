/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.micaConfig.MicaConfigService;
import org.obiba.mica.search.rest.AggregationYamlParser;
import org.obiba.mica.search.rest.EsQueryResultParser;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;

import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

public abstract class AbstractDocumentQuery {

  private static final String AGG_TOTAL_COUNT = "totalCount";

  @Inject
  private Client client;

  @Inject
  private AggregationYamlParser aggregationYamlParser;

  @Inject
  MicaConfigService micaConfigService;

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  protected QueryDto queryDto;

  private QueryResultDto resultDto;

  public QueryDto getQuery() {
    return queryDto;
  }

  public void initialize(QueryDto query) {
    queryDto = query;
    resultDto = null;
  }

  public QueryResultDto getResultQuery() {
    return resultDto;
  }

  public abstract String getSearchIndex();

  public abstract String getSearchType();

  public abstract Resource getAggregationsDescription();

  /**
   * Executes query to extract study IDs from the aggregation results
   *
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> queryStudyIds() throws IOException {
    if(queryDto == null) return null;

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
        .setTypes(getSearchType()) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(QueryDtoParser.newParser().parse(queryDto)) //
        .setNoFields();

    aggregationYamlParser.getAggregations(getJoinFields()).forEach(requestBuilder::addAggregation);
    log.info("Request: {}", requestBuilder);
    SearchResponse response = requestBuilder.execute().actionGet();
    log.info("Response: {}", response);
    List<String> ids = Lists.newArrayList();
    response.getAggregations()
        .forEach(aggregation -> ((Terms) aggregation).getBuckets().forEach(bucket -> ids.add(bucket.getKey())));

    return ids;
  }

  /**
   * Executes a 'match all' query to retrieve documents and aggregations
   * @throws IOException
   */
  public void query() throws IOException {
    execute(QueryBuilders.matchAllQuery(), true);
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations
   * @param studyIds
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> query(List<String> studyIds) throws IOException {
    if(queryDto == null) throw new IllegalArgumentException("Document query cannot have a NULL query.");
    addStudyIdFilters(studyIds);
    return execute(QueryDtoParser.newParser().parse(queryDto), true);
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations, former being optional dependinggg on the type of
   * query.
   * @param studyIds
   * @throws IOException
   */
  public void queryAggrations(List<String> studyIds) throws IOException {
    queryAggregations(studyIds, true);
  }

  protected void queryAggregations(List<String> studyIds, boolean details) throws IOException {
    if(queryDto == null) {
      execute(QueryDtoParser.newParser().parse(createStudyIdFilters(studyIds)), details);
    } else {
      addStudyIdFilters(studyIds);
      execute(QueryDtoParser.newParser().parse(queryDto), details);
    }
  }

  /**
   * Executes a query to retrieve documents and aggregations
   * @param queryBuilder
   * @param details
   * @return
   * @throws IOException
   */
  protected List<String> execute(QueryBuilder queryBuilder, boolean details) throws IOException {
    if(queryBuilder == null) return null;

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
        .setTypes(getSearchType()) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(queryBuilder) //
        .addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT));

    if(ignoreFields()) requestBuilder.setNoFields();

    aggregationYamlParser.setLocales(micaConfigService.getConfig().getLocales());
    aggregationYamlParser.getAggregations(getAggregationsDescription()).forEach(requestBuilder::addAggregation);

    log.info("Request: {}", requestBuilder.toString());
    SearchResponse response = requestBuilder.execute().actionGet();
    log.info("Response: {}", response.toString());
    if (response.getHits().totalHits() > 0) {
      QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int) response.getHits().getTotalHits());
      if(details) processHits(builder, response.getHits());
      processAggregations(builder, response.getAggregations());
      resultDto = builder.build();
    }

    return getResponseStudyIds(resultDto.getAggsList());
  }

  /**
   * Returning 'false' will include documents in the query result
   * @return
   */
  protected boolean ignoreFields() {
    return true;
  }

  /**
   * Creates domain documents DTOs
   * @param builder
   * @param hits
   * @throws IOException
   */
  protected abstract void processHits(QueryResultDto.Builder builder, SearchHits hits) throws IOException;

  /**
   * Creates domain aggregation DTOs
   * @param builder
   * @param aggregations
   */
  protected void processAggregations(QueryResultDto.Builder builder, Aggregations aggregations) {
    EsQueryResultParser parser = EsQueryResultParser.newParser();
    builder.addAllAggs(parser.parseAggregations(aggregations));
    builder.setTotalCount(parser.getTotalCount());
  }

  /**
   * Returns study ID fields (aggregtaion fields)
   * @return
   */
  protected abstract Properties getJoinFields();

  protected void addStudyIdFilters(List<String> studyIds) {
    if(studyIds == null || studyIds.size() == 0) return;
    queryDto = QueryDtoHelper.addShouldBoolFilters(queryDto, createTermFilterQueryDtos(studyIds));
  }

  protected QueryDto createStudyIdFilters(List<String> studyIds) {
    return QueryDto.newBuilder().setSize(10).setFrom(0).setDetailed(false).setFilteredQuery(
        MicaSearch.FilteredQueryDto.newBuilder()
            .setFilter(MicaSearch.BoolFilterQueryDto.newBuilder().addAllShould(createTermFilterQueryDtos(studyIds))))
        .build();
  }

  private List<MicaSearch.FilterQueryDto> createTermFilterQueryDtos(List<String> studyIds) {
    List<MicaSearch.FilterQueryDto> filters = Lists.newArrayList();
    Properties joinFields = getJoinFields();
    for(Map.Entry<Object, Object> entry : joinFields.entrySet()) {
      filters.add(QueryDtoHelper.createTermFilter(entry.getKey().toString(), studyIds));
    }
    return filters;
  }

  /**
   * Iterate through response aggregations and retrieve the studyIds that were included
   *
   * @param aggDtos
   * @return
   */
  private List<String> getResponseStudyIds(List<MicaSearch.AggregationResultDto> aggDtos) {
    Properties joinFields = getJoinFields();
    List<String> ids = Lists.newArrayList();
    aggDtos.forEach(aggDto -> {
      if(joinFields.getProperty(AggregationYamlParser.unformatName(aggDto.getAggregation())) != null) {
        aggDto.getExtension(MicaSearch.TermsAggregationResultDto.terms).forEach(term -> ids.add(term.getKey()));
      }
    });

    return ids;
  }

}
