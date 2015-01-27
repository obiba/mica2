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
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilder;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.aggregations.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.AggregationMetaDataResolver;
import org.obiba.mica.search.aggregations.AggregationYamlParser;
import org.obiba.mica.search.rest.EsQueryResultParser;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.obiba.mica.search.queries.AbstractDocumentQuery.Mode.COVERAGE;
import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DETAIL;
import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DIGEST;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

public abstract class AbstractDocumentQuery {

  private static final String AGG_TOTAL_COUNT = "totalCount";

  @Inject
  protected Client client;

  @Inject
  protected AggregationYamlParser aggregationYamlParser;

  @Inject
  MicaConfigService micaConfigService;

  @Inject
  AggregationMetaDataResolver aggregationTitleResolver;

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  public enum Mode {
    SEARCH,
    COVERAGE
  }

  public enum Scope {
    NONE,
    DIGEST,
    DETAIL
  }

  protected Mode mode = Mode.SEARCH;

  protected QueryDto queryDto;

  protected QueryResultDto resultDto;

  private String locale;

  public QueryDto getQuery() {
    return queryDto;
  }

  public boolean hasQueryFilters() {
    return QueryDtoHelper.hasQuery(queryDto);
  }

  public void initialize(QueryDto query, String localeName, Mode mode) {
    this.mode = mode;
    locale = localeName;
    queryDto = QueryDtoHelper
      .ensureQueryStringDtoFields(query, locale, getLocalizedQueryStringFields(), getQueryStringFields());
    resultDto = null;
  }

  public QueryResultDto getResultQuery() {
    return resultDto;
  }

  public abstract String getSearchIndex();

  public abstract String getSearchType();

  public abstract Stream<String> getLocalizedQueryStringFields();

  public Stream<String> getQueryStringFields() {
    return null;
  }

  protected abstract Resource getAggregationsDescription();

  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Lists.newArrayList();
  }

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
    if(queryDto == null) return null;
    return queryStudyIds(queryDto);
  }

  /**
   * Used on a document query to extract studsy IDs without details
   *
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
      .setSearchType(SearchType.COUNT) //
      .setQuery(QueryDtoParser.newParser().parse(queryDto)) //
      .setNoFields();

    aggregationYamlParser.getAggregations(getJoinFieldsAsProperties()).forEach(requestBuilder::addAggregation);
    log.info("Request: {}", requestBuilder);
    SearchResponse response = requestBuilder.execute().actionGet();
    List<String> ids = Lists.newArrayList();

    response.getAggregations().forEach(aggregation -> ((Terms) aggregation).getBuckets().stream().forEach(bucket -> {
      if(bucket.getDocCount() > 0){
        ids.add(bucket.getKey());
      }
    }));

    return ids.stream().distinct().collect(Collectors.toList());
  }

  private Properties getJoinFieldsAsProperties() {
    Properties props = new Properties();

    try {
      props.load(new StringReader(getJoinFields().stream().reduce((t, s) -> t + "=\r" + s).get()));
    } catch(IOException e) {
      log.error("Failed to create properties from query join fields: {}", e);
    }

    return props;
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations
   *
   * @param studyIds
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    QueryDto tempQueryDto = queryDto == null ? createStudyIdFilters(studyIds) : addStudyIdFilters(studyIds);
    return mode == COVERAGE ?
      executeCoverage(tempQueryDto, tempQueryDto.getFrom(), tempQueryDto.getSize(), DIGEST, counts)
      : execute(tempQueryDto, tempQueryDto.getFrom(), tempQueryDto.getSize(), scope, counts);
  }

  /**
   * Executes a query to retrieve documents and aggregations
   *
   * @param queryDto
   * @param from
   * @param size
   * @param scope
   * @param counts
   * @return
   * @throws IOException
   */
  protected List<String> execute(QueryDto queryDto, int from, int size, Scope scope, CountStatsData counts)
    throws IOException {
    if(queryDto == null) return null;

    QueryDtoParser queryDtoParser = QueryDtoParser.newParser();
    aggregationTitleResolver.registerProviders(getAggregationMetaDataProviders());
    aggregationTitleResolver.refresh();

    SearchRequestBuilder defaultRequestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(scope == DETAIL ? SearchType.DFS_QUERY_THEN_FETCH : SearchType.COUNT) //
      .setQuery(QueryBuilders.matchAllQuery()) //
      .setFrom(from) //
      .setSize(size) //
      .setNoFields().addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT)); //

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(scope == DETAIL ? SearchType.DFS_QUERY_THEN_FETCH : SearchType.COUNT) //
      .setQuery(queryDtoParser.parse(queryDto)) //
      .setFrom(from) //
      .setSize(size) //
      .addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT)); // ;

    if(ignoreFields()) requestBuilder.setNoFields();

    SortBuilder sortBuilder = queryDtoParser.parseSort(queryDto);

    if(sortBuilder != null) requestBuilder.addSort(queryDtoParser.parseSort(queryDto));

    aggregationYamlParser.setLocales(micaConfigService.getConfig().getLocales());
    Map<String, Properties> subAggregations = Maps.newHashMap();
    Properties aggregationProperties = getAggregationsProperties();
    if(queryDto.getAggsByCount() > 0) {
      queryDto.getAggsByList().forEach(field -> subAggregations.put(field, aggregationProperties));
    }

    aggregationYamlParser.getAggregations(getAggregationsDescription(), subAggregations).forEach(agg -> {
      defaultRequestBuilder.addAggregation(agg);
      requestBuilder.addAggregation(agg);
    });

    aggregationYamlParser.getAggregations(aggregationProperties).forEach(agg -> {
      defaultRequestBuilder.addAggregation(agg);
      requestBuilder.addAggregation(agg);
    });

    log.info("Request: {}", requestBuilder.toString());

    try {
      List<SearchResponse> responses = Stream
        .of(client.prepareMultiSearch().add(defaultRequestBuilder).add(requestBuilder).execute().actionGet())
        .map(MultiSearchResponse::getResponses).flatMap((d) -> Stream.of(d)).map(MultiSearchResponse.Item::getResponse)
        .collect(Collectors.toList());

      SearchResponse aggResponse = responses.get(0);
      SearchResponse response = responses.get(1);
      log.info("Response: {}", response);

      if(response == null) {
        return null;
      }

      QueryResultDto.Builder builder = QueryResultDto.newBuilder()
        .setTotalHits((int) response.getHits().getTotalHits());

      if(scope == DETAIL) processHits(builder, response.getHits(), scope, counts);

      processAggregations(builder, aggResponse.getAggregations(), response.getAggregations());
      resultDto = builder.build();

      return getResponseStudyIds(resultDto.getAggsList());
    } catch(IndexMissingException e) {
      log.error("Missing index: {}", e.getMessage(), e);
      return null;
    } finally {
      aggregationTitleResolver.unregisterProviders(getAggregationMetaDataProviders());
    }

  }

  /**
   * Executes a query to retrieve documents and aggregations for coverage
   *
   * @param queryDto
   * @param from
   * @param size
   * @param scope
   * @param counts
   * @return
   * @throws IOException
   */
  protected List<String> executeCoverage(QueryDto queryDto, int from, int size, Scope scope, CountStatsData counts)
    throws IOException {
    if(queryDto == null) return null;

    QueryDtoParser queryDtoParser = QueryDtoParser.newParser();
    aggregationTitleResolver.registerProviders(getAggregationMetaDataProviders());
    aggregationTitleResolver.refresh();

    SearchRequestBuilder defaultRequestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.COUNT) //
      .setQuery(QueryBuilders.matchAllQuery()) //
      .setFrom(from) //
      .setSize(size) //
      .setNoFields().addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT));

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.COUNT) //
      .setQuery(queryDtoParser.parse(queryDto)) //
      .setFrom(from) //
      .setSize(size) //
      .addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT));

    if(ignoreFields()) requestBuilder.setNoFields();

    SortBuilder sortBuilder = queryDtoParser.parseSort(queryDto);
    if(sortBuilder != null) requestBuilder.addSort(queryDtoParser.parseSort(queryDto));

    aggregationYamlParser.setLocales(micaConfigService.getConfig().getLocales());
    Map<String, Properties> subAggregations = Maps.newHashMap();
    Properties aggregationProperties = getAggregationsProperties();

    if(queryDto != null && queryDto.getAggsByCount() > 0) {
      queryDto.getAggsByList().forEach(field -> subAggregations.put(field, aggregationProperties));
    }

    aggregationYamlParser.getAggregations(getAggregationsDescription(), subAggregations)
      .forEach(agg -> {
        requestBuilder.addAggregation(agg);
        defaultRequestBuilder.addAggregation(agg);
      });

    aggregationYamlParser.getAggregations(aggregationProperties).forEach(agg -> {
      requestBuilder.addAggregation(agg);
      defaultRequestBuilder.addAggregation(agg);
    });

    log.info("Request: {}", requestBuilder.toString());

    try {
      List<SearchResponse> responses = Stream
        .of(client.prepareMultiSearch().add(defaultRequestBuilder).add(requestBuilder).execute().actionGet())
        .map(MultiSearchResponse::getResponses).flatMap((d) -> Stream.of(d)).map(MultiSearchResponse.Item::getResponse)
        .collect(Collectors.toList());

      SearchResponse defaultResponse = responses.get(0);
      SearchResponse response = responses.get(1);

      log.info("Response: {}", response.toString());
      QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int) response.getHits().getTotalHits());

      if(scope == DETAIL) processHits(builder, response.getHits(), scope, counts);

      processAggregations(builder, defaultResponse.getAggregations(), response.getAggregations());
      resultDto = builder.build();
      return getResponseStudyIds(resultDto.getAggsList());
    } catch(IndexMissingException e) {
      log.error("Missing index: {}", e.getMessage(), e);
      return null;
    }
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
  protected abstract void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope,
    CountStatsData counts) throws IOException;

  /**
   * Creates domain aggregation DTOs
   *
   * @param builder
   * @param aggregations
   */
  protected void processAggregations(QueryResultDto.Builder builder, Aggregations defaults, Aggregations aggregations) {
    EsQueryResultParser parser = EsQueryResultParser.newParser(aggregationTitleResolver, locale);
    builder.addAllAggs(defaults == null //
      ? parser.parseAggregations(aggregations) //
      : parser.parseAggregations(defaults, aggregations)); //
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

    return QueryDtoHelper.addTermFilters(QueryDto.newBuilder(queryDto).build(),
      QueryDtoHelper.createTermFilters(getJoinFields(), studyIds), QueryDtoHelper.BoolQueryType.MUST);
  }

  protected QueryDto createStudyIdFilters(List<String> studyIds) {
    return QueryDtoHelper.createTermFiltersQuery(getJoinFields(), studyIds, QueryDtoHelper.BoolQueryType.MUST);
  }

  public abstract Map<String, Integer> getStudyCounts();

  /**
   * Iterate through response aggregations and retrieve the studyIds that were included
   *
   * @param aggDtos
   * @return
   */
  protected List<String> getResponseStudyIds(List<MicaSearch.AggregationResultDto> aggDtos) {
    return getResponseDocumentIds(getJoinFields(), aggDtos);
  }

  protected Map<String, Integer> getDocumentCounts(String joinField) {
    if(resultDto == null) return Maps.newHashMap();
    return resultDto.getAggsList().stream() //
      .filter(agg -> joinField.equals(AggregationYamlParser.unformatName(agg.getAggregation()))) //
      .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
      .flatMap((d) -> d.stream()) //
      .filter(s -> s.getCount() > 0) //
      .collect(Collectors.toMap(MicaSearch.TermsAggregationResultDto::getKey, term -> term.getCount()));
  }

  protected List<String> getResponseDocumentIds(List<String> fields, List<MicaSearch.AggregationResultDto> aggDtos) {
    List<String> ids = aggDtos.stream() //
      .filter(agg -> fields.contains(AggregationYamlParser.unformatName(agg.getAggregation()))) //
      .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
      .flatMap((d) -> d.stream()) //
      .filter(s -> s.getCount() > 0) //
      .map(MicaSearch.TermsAggregationResultDto::getKey) //
      .collect(Collectors.toList()); //
    return ids;
  }

}
