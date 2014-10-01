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
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
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
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.rest.AggregationYamlParser;
import org.obiba.mica.search.rest.EsQueryResultParser;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

public abstract class AbstractDocumentQuery {

  private static final String AGG_TOTAL_COUNT = "totalCount";

  private static final int DEFAULT_FROM = 0;

  private static final int DEFAULT_SIZE = 10;

  @Inject
  private Client client;

  @Inject
  private AggregationYamlParser aggregationYamlParser;

  @Inject
  MicaConfigService micaConfigService;

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  protected QueryDto queryDto;

  protected QueryResultDto resultDto;

  public QueryDto getQuery() {
    return queryDto;
  }

  public boolean hasQueryFilters() {
    return queryDto != null && queryDto.hasFilteredQuery() && queryDto.getFilteredQuery().hasFilter();
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

  protected abstract Resource getAggregationsDescription();

  @Nullable
  protected Properties getAggregationsProperties() {
    return null;
  }

  /**
   * Executes query to extract study IDs from the aggregation results
   *
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> queryStudyIds() throws IOException {
    if (queryDto == null) return null;
    return queryStudyIds(queryDto);
  }

  /**
   * Used on a document query to extract studsy IDs without details
   * @param studyIds
   * @return
   * @throws IOException
   */
  public List<String> queryStudyIds(List<String> studyIds) throws IOException {
    return queryStudyIds(queryDto == null ? createStudyIdFilters(studyIds) : addStudyIdFilters(studyIds));
  }

  protected List<String> queryStudyIds(QueryDto queryDto) throws IOException {
    if(queryDto == null) return null;

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
        .setTypes(getSearchType()) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(QueryDtoParser.newParser().parse(queryDto)) //
        .setNoFields();

    aggregationYamlParser.getAggregations(getJoinFieldsAsProperties()).forEach(requestBuilder::addAggregation);
    log.info("Request: {}", requestBuilder);
    SearchResponse response = requestBuilder.execute().actionGet();
    List<String> ids = Lists.newArrayList();
    response.getAggregations()
        .forEach(aggregation -> ((Terms) aggregation).getBuckets().forEach(bucket -> ids.add(bucket.getKey())));

    return ids;
  }

  private Properties getJoinFieldsAsProperties() {
    Properties props = new Properties();
    try {
      props.load(new StringReader(getJoinFields().stream().reduce((t, s) -> t + "=\r" + s).get()));
    } catch (IOException e) {
      log.error("Failed to create properties from query join fields: {}", e);
    }

    return props;
  }

  /**
   * Executes a 'match all' query to retrieve documents and aggregations
   *
   * @throws IOException
   */
  public void query(int from, int size, CountStatsData counts) throws IOException {
    execute(QueryBuilders.matchAllQuery(), from, size, true, counts);
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations
   *
   * @param studyIds
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> query(List<String> studyIds, CountStatsData counts) throws IOException {
    QueryDto tempQueryDto = queryDto == null ? createStudyIdFilters(studyIds) : addStudyIdFilters(studyIds);
    return execute(QueryDtoParser.newParser().parse(tempQueryDto), tempQueryDto.getFrom(), tempQueryDto.getSize(), true,
        counts);
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations, former being optional dependinggg on the type of
   * query.
   *
   * @param studyIds
   * @throws IOException
   */
  public List<String> queryAggrations(List<String> studyIds) throws IOException {
    return queryAggregations(studyIds, true);
  }

  protected List<String> queryAggregations(List<String> studyIds, boolean details) throws IOException {
    QueryDto tempQueryDto = queryDto == null ? createStudyIdFilters(studyIds) : addStudyIdFilters(studyIds);
    return execute(QueryDtoParser.newParser().parse(tempQueryDto), tempQueryDto.getFrom(), tempQueryDto.getSize(),
        details, null);
  }

  /**
   * Executes a query to retrieve documents and aggregations
   *
   * @param queryBuilder
   * @param details
   * @return
   * @throws IOException
   */
  protected List<String> execute(QueryBuilder queryBuilder, int from, int size, boolean details, CountStatsData counts) throws IOException {
    if(queryBuilder == null) return null;

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
        .setTypes(getSearchType()) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(queryBuilder) //
        .setFrom(from) //
        .setSize(size) //
        .addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT));

    if(ignoreFields()) requestBuilder.setNoFields();

    aggregationYamlParser.setLocales(micaConfigService.getConfig().getLocales());
    aggregationYamlParser.getAggregations(getAggregationsDescription()).forEach(requestBuilder::addAggregation);
    aggregationYamlParser.getAggregations(getAggregationsProperties()).forEach(requestBuilder::addAggregation);

    log.info("Request: {}", requestBuilder.toString());
    SearchResponse response = requestBuilder.execute().actionGet();
    if(response.getHits().totalHits() > 0) {
      QueryResultDto.Builder builder = QueryResultDto.newBuilder()
          .setTotalHits((int) response.getHits().getTotalHits());
      if(details) processHits(builder, response.getHits(), counts);
      processAggregations(builder, response.getAggregations());
      resultDto = builder.build();
    }

    return resultDto == null ? null : getResponseStudyIds(resultDto.getAggsList());
  }

  /**
   * Returning 'false' will include documents in the query result
   *
   * @return
   */
  protected boolean ignoreFields() {
    return true;
  }

  /**
   * Creates domain documents DTOs
   *
   * @param builder
   * @param hits
   * @throws IOException
   */
  protected abstract void processHits(QueryResultDto.Builder builder, SearchHits hits, CountStatsData counts) throws
      IOException;

  /**
   * Creates domain aggregation DTOs
   *
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
   *
   * @return
   */
  protected abstract List<String> getJoinFields();

  protected QueryDto addStudyIdFilters(List<String> studyIds) {
    if(studyIds == null || studyIds.size() == 0) return queryDto;
    return  QueryDtoHelper.addShouldBoolFilters(QueryDto.newBuilder(queryDto).build(), createTermFilterQueryDtos(studyIds));
  }

  protected QueryDto createStudyIdFilters(List<String> studyIds) {
    QueryDto.Builder builder = QueryDto.newBuilder().setSize(DEFAULT_SIZE).setFrom(DEFAULT_FROM);
    if (studyIds != null && studyIds.size() > 0) {
      MicaSearch.BoolFilterQueryDto.Builder boolBuilder = MicaSearch.BoolFilterQueryDto.newBuilder();
      boolBuilder.addAllShould(createTermFilterQueryDtos(studyIds));
      builder.setFilteredQuery(MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolBuilder));
    }

    return builder.build();
  }

  private List<MicaSearch.FilterQueryDto> createTermFilterQueryDtos(List<String> studyIds) {
    return  getJoinFields() //
        .stream() //
        .map(field -> QueryDtoHelper.createTermFilter(field, studyIds)) //
        .collect(Collectors.toList());
  }

  public abstract Map<String, Integer> getStudyCounts();

  protected Map<String, Integer> getStudyCounts(String joinField) {
    if (resultDto == null) return Maps.newHashMap();
    return resultDto.getAggsList().stream() //
        .filter(agg -> joinField.equals(AggregationYamlParser.unformatName(agg.getAggregation()))) //
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
        .flatMap((d) -> d.stream()) //
        .collect(Collectors.toMap(MicaSearch.TermsAggregationResultDto::getKey, term -> term.getCount()));
  }

  /**
   * Iterate through response aggregations and retrieve the studyIds that were included
   *
   * @param aggDtos
   * @return
   */
  protected List<String> getResponseStudyIds(List<MicaSearch.AggregationResultDto> aggDtos) {
    List<String> ids =
     aggDtos.stream() //
        .filter(agg -> getJoinFields().contains(AggregationYamlParser.unformatName(agg.getAggregation()))) //
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
        .flatMap((d) -> d.stream()).map(MicaSearch.TermsAggregationResultDto::getKey).collect(Collectors.toList());
    return ids;
  }

}
