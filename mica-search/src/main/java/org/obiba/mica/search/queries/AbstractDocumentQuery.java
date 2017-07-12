/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.sort.SortBuilder;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.micaConfig.service.helper.AggregationAliasHelper;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.DocumentQueryHelper;
import org.obiba.mica.search.aggregations.AggregationMetaDataResolver;
import org.obiba.mica.search.aggregations.AggregationYamlParser;
import org.obiba.mica.search.queries.protobuf.QueryDtoHelper;
import org.obiba.mica.search.queries.protobuf.QueryDtoWrapper;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.jsonpath.JsonPath.using;
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
  protected MicaConfigService micaConfigService;

  @Inject
  protected TaxonomyService taxonomyService;

  @Inject
  protected SubjectAclService subjectAclService;

  @Inject
  AggregationMetaDataResolver aggregationTitleResolver;

  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  public enum Mode {
    SEARCH,  // search for documents (with results and aggregations)
    LIST,    // list documents (with results but no aggregations)
    COVERAGE // search for documents coverage of classifications (with aggregations but no results)
  }

  public enum Scope {
    NONE,
    DIGEST,
    DETAIL
  }

  protected Mode mode = Mode.SEARCH;

  private QueryWrapper queryWrapper;

  protected QueryResultDto resultDto;

  private String locale;

  public QueryBuilder getQueryBuilder() {
    return queryWrapper.getQueryBuilder();
  }

  public void setQueryBuilder(QueryBuilder queryBuilder) {
    queryWrapper.setQueryBuilder(queryBuilder);
  }

  public boolean hasQueryBuilder() {
    return queryWrapper.hasQueryBuilder();
  }

  protected List<String> getAggregationGroupBy() {
    return hasQueryBuilder() ? queryWrapper.getAggregationBuckets() : Lists.newArrayList();
  }

  private int getFrom() {
    return queryWrapper.getFrom();
  }

  private int getSize() {
    return queryWrapper.getSize();
  }

  private List<SortBuilder> getSortBuilders() {
    return queryWrapper.getSortBuilders();
  }

  public void initialize(QueryDto query, String locale, Mode mode) {
    QueryDto queryDto = QueryDtoHelper
      .ensureQueryStringDtoFields(query, locale, getLocalizedQueryStringFields(), getQueryStringFields());
    initialize(new QueryDtoWrapper(queryDto), locale, mode);
  }

  public void initialize(@Nullable QueryWrapper queryWrapper, String locale, Mode mode) {
    this.mode = mode;
    this.locale = locale;
    this.queryWrapper = queryWrapper == null ? new EmptyQueryWrapper() : queryWrapper;
    resultDto = null;
  }

  public QueryResultDto getResultQuery() {
    return resultDto;
  }

  public abstract String getSearchIndex();

  public abstract String getSearchType();

  public abstract QueryBuilder getAccessFilter();

  public abstract Stream<String> getLocalizedQueryStringFields();

  /**
   * Verifies if the query has any criteria on the primary key (ID).
   *
   * @return true/false
   */
  public boolean hasPrimaryKeyCriteria() {
    if (hasQueryBuilder()) {
      Object jsonContent = Configuration.defaultConfiguration().jsonProvider().parse(getQueryBuilder().toString());
      ReadContext context = using(Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST)).parse(jsonContent);
      List<String> ids = context.read("$..id");
      return ids.size() > 0;
    }

    return false;
  }

  protected QueryBuilder updateJoinKeyQuery(QueryBuilder queryBuilder) {
    return queryBuilder;
  }

  protected DocumentQueryJoinKeys processJoinKeys(SearchResponse response) {
    return DocumentQueryHelper.processStudyJoinKey(response);
  }

  public Stream<String> getQueryStringFields() {
    return null;
  }

  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Lists.newArrayList();
  }

  @Nullable
  protected Properties getAggregationsProperties(List<String> filter) {
    return null;
  }

  protected Properties getAggregationsProperties(List<String> filter, Taxonomy taxonomy) {
    Properties properties = new Properties();
    if(mode != Mode.LIST && filter != null) {
      List<Pattern> patterns = filter.stream().map(Pattern::compile).collect(Collectors.toList());
      taxonomy.getVocabularies().forEach(vocabulary -> {
        String field = vocabulary.getAttributes().containsKey("field")
          ? vocabulary.getAttributeValue("field")
          : vocabulary.getName().replace('-', '.');

        if(patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(field).matches())) {
          boolean multipleTypes = null != properties.get(field);
          properties.put(field, "");
          String type = vocabulary.getAttributeValue("type");
          if("integer".equals(type) || "decimal".equals(type)) {
            if("true".equals(vocabulary.getAttributeValue("range"))) {
              addProperty(properties, field + AggregationYamlParser.TYPE, AggregationYamlParser.AGG_RANGE, multipleTypes);
              properties.put(field + AggregationYamlParser.RANGES,
                vocabulary.getTerms().stream().map(TaxonomyEntity::getName).collect(Collectors.joining(",")));
            } else {
              addProperty(properties, field + AggregationYamlParser.TYPE, AggregationYamlParser.AGG_STATS, multipleTypes);
            }
          }
          String alias = vocabulary.getAttributeValue("alias");
          if(!Strings.isNullOrEmpty(alias)) {
            addProperty(properties, field + AggregationYamlParser.ALIAS, alias, multipleTypes);
          }
        }
      });
    }
    return properties;
  }

  private void addProperty(Properties properties, String property, String value, boolean multipleTypes) {
    if (multipleTypes) {
      String currentTypes = properties.get(property).toString();
      properties.put(property, currentTypes + "," + value);
    } else {
      properties.put(property, value);
    }
  }

  private Properties getFilteredAggregationsProperties() {
    Properties properties = getAggregationsProperties(queryWrapper.getAggregations());

    // make sure the buckets are part of the aggregations
    if(properties != null) {
      getAggregationGroupBy().stream().filter(b -> !properties.containsKey(b))
        .forEach(b -> properties.put(b, ""));
    }

    return properties;
  }

  /**
   * Executes query to extract study IDs from the aggregation results
   *
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> queryStudyIds() {
    return queryStudyIds(queryWrapper.getQueryBuilder());
  }

  /**
   * Used on a document query to extract studsy IDs without details
   *
   * @param studyIds
   * @return
   * @throws IOException
   */
  public List<String> queryStudyIds(List<String> studyIds) {
    return queryStudyIds(newStudyIdQuery(studyIds));
  }

  protected List<String> queryStudyIds(QueryBuilder queryBuilder) {
    if(queryBuilder == null) return null;

    queryBuilder = updateJoinKeyQuery(queryBuilder);
    QueryBuilder accessFilter = getAccessFilter();

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.QUERY_THEN_FETCH) //
      .setSize(0) //
      .setQuery(
        accessFilter == null ? queryBuilder : QueryBuilders.boolQuery().must(queryBuilder).must(accessFilter)) //
      .setNoFields();

    aggregationYamlParser.getAggregations(getJoinFieldsAsProperties()).forEach(requestBuilder::addAggregation);
    log.debug("Request /{}/{}", getSearchIndex(), getSearchType());
    if(log.isTraceEnabled()) log.trace("Request /{}/{}: {}", getSearchIndex(), getSearchType(), requestBuilder);
    try {
      SearchResponse response = requestBuilder.execute().actionGet();
      log.debug("Response /{}/{}", getSearchIndex(), getSearchType());
      if(log.isTraceEnabled()) log.trace("Response /{}/{}: totalHits={}", getSearchIndex(), getSearchType(), response.getHits().getTotalHits());

      DocumentQueryJoinKeys joinKeys = processJoinKeys(response);

      return joinKeys.studyIds.stream().distinct().collect(Collectors.toList());
    } catch(IndexNotFoundException e) {
      return Collections.emptyList(); //ignoring
    }
  }

  private Properties getJoinFieldsAsProperties() {
    Properties props = new Properties();

    try {
      props.load(new StringReader(getAggJoinFields().stream().reduce((t, s) -> t + "=\r" + s).get()));
    } catch(IOException e) {
      log.error("Failed to create properties from query join fields: {}", e);
    }

    return props;
  }

  /**
   * Executes a filtered query to retrieve documents and aggregations.
   *
   * @param studyIds
   * @return List of study IDs
   * @throws IOException
   */
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    QueryBuilder tempQuery = newStudyIdQuery(studyIds);
    return mode == COVERAGE
      ? executeCoverage(tempQuery, null, DIGEST, counts, getAggregationGroupBy())
      : execute(tempQuery, getSortBuilders(), getFrom(), getSize(), scope, counts, getAggregationGroupBy());
  }

  /**
   * Executes a query to retrieve documents and aggregations.
   *
   * @param query
   * @param sortBuilder
   * @param from
   * @param size
   * @param scope
   * @param counts
   * @param aggregationGroupBy
   * @return
   * @throws IOException
   */
  protected List<String> execute(QueryBuilder query, List<SortBuilder> sortBuilder, int from, int size, Scope scope,
    CountStatsData counts, List<String> aggregationGroupBy) throws IOException {
    if(query == null) return null;

    aggregationTitleResolver.registerProviders(getAggregationMetaDataProviders());
    aggregationTitleResolver.refresh();

    QueryBuilder accessFilter = getAccessFilter();

    SearchRequestBuilder defaultRequestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.QUERY_THEN_FETCH) //
      .setQuery(accessFilter == null
        ? QueryBuilders.matchAllQuery()
        : QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).must(accessFilter)) //
      .setFrom(from) //
      .setSize(scope == DETAIL ? size : 0) //
      .setNoFields().addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT)); //

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.QUERY_THEN_FETCH) //
      .setQuery(accessFilter == null ? query : QueryBuilders.boolQuery().must(query).must(accessFilter)) //
      .setFrom(from) //
      .setSize(scope == DETAIL ? size : 0) //
      .addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT)); // ;

    if(ignoreFields()) requestBuilder.setNoFields();

    if(sortBuilder != null) sortBuilder.forEach(requestBuilder::addSort);

    appendAggregations(defaultRequestBuilder, requestBuilder, aggregationGroupBy);

    log.debug("Request /{}/{}", getSearchIndex(), getSearchType());
    if(log.isTraceEnabled()) log.trace("Request /{}/{}: {}", getSearchIndex(), getSearchType(), requestBuilder.toString());

    try {
      List<SearchResponse> responses = Stream
        .of(client.prepareMultiSearch().add(defaultRequestBuilder).add(requestBuilder).execute().actionGet())
        .map(MultiSearchResponse::getResponses).flatMap(Stream::of).map(MultiSearchResponse.Item::getResponse)
        .collect(Collectors.toList());

      SearchResponse aggResponse = responses.get(0);
      SearchResponse response = responses.get(1);

      log.debug("Response /{}/{}", getSearchIndex(), getSearchType());
      if(response == null) {
        if(log.isTraceEnabled()) log.trace("Response /{}/{}: totalHits=null", getSearchIndex(), getSearchType());
        return null;
      }
      if(log.isTraceEnabled()) log.trace("Response /{}/{}: totalHits={}", getSearchIndex(), getSearchType(), response.getHits().totalHits());

      QueryResultDto.Builder builder = QueryResultDto.newBuilder()
        .setTotalHits((int) response.getHits().getTotalHits());

      if(scope == DETAIL) processHits(builder, response.getHits(), scope, counts);

      processAggregations(builder, aggResponse.getAggregations(), response.getAggregations());
      resultDto = builder.build();

      return getResponseStudyIds(resultDto.getAggsList());
    } catch(IndexNotFoundException e) {
      return null; //ignoring
    } finally {
      aggregationTitleResolver.unregisterProviders(getAggregationMetaDataProviders());
    }
  }

  /**
   * Executes a query to retrieve documents and aggregations for coverage.
   *
   * @param queryBuilder
   * @param sortBuilder
   * @param scope
   * @param counts
   * @param aggregationGroupBy
   * @return
   * @throws IOException
   */
  protected List<String> executeCoverage(QueryBuilder queryBuilder, SortBuilder sortBuilder, Scope scope,
    CountStatsData counts, List<String> aggregationGroupBy) throws IOException {
    if(queryBuilder == null) return null;

    aggregationTitleResolver.registerProviders(getAggregationMetaDataProviders());
    aggregationTitleResolver.refresh();

    QueryBuilder accessFilter = getAccessFilter();

    SearchRequestBuilder defaultRequestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.QUERY_THEN_FETCH) //
      .setQuery(accessFilter == null
        ? QueryBuilders.matchAllQuery()
        : QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).must(accessFilter)) //
      .setFrom(0) //
      .setSize(0) // no results needed for a coverage
      .setNoFields().addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT));

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.QUERY_THEN_FETCH) //
      .setQuery(
        accessFilter == null ? queryBuilder : QueryBuilders.boolQuery().must(queryBuilder).must(accessFilter)) //
      .setFrom(0) //
      .setSize(0) // no results needed for a coverage
      .setNoFields().addAggregation(AggregationBuilders.global(AGG_TOTAL_COUNT));

    if(sortBuilder != null) requestBuilder.addSort(sortBuilder);

    appendAggregations(defaultRequestBuilder, requestBuilder, aggregationGroupBy);

    log.debug("Request /{}/{}", getSearchIndex(), getSearchType());
    if(log.isTraceEnabled()) log.trace("Request /{}/{}: {}", getSearchIndex(), getSearchType(), requestBuilder.toString());

    MultiSearchResponse.Item[] responses = client.prepareMultiSearch().add(defaultRequestBuilder).add(requestBuilder)
      .execute().actionGet().getResponses();

    SearchResponse defaultResponse = responses[0].getResponse();
    SearchResponse response = responses[1].getResponse();

    log.debug("Response /{}/{}", getSearchIndex(), getSearchType());
    if(log.isTraceEnabled()) log.trace("Response /{}/{}: totalHits={}", getSearchIndex(), getSearchType(), response.getHits().getTotalHits());

    List<String> rval = null;
    if (response != null) {
      QueryResultDto.Builder builder = QueryResultDto.newBuilder()
        .setTotalHits((int) response.getHits().getTotalHits());
      if (scope == DETAIL) processHits(builder, response.getHits(), scope, counts);
      processAggregations(builder, defaultResponse.getAggregations(), response.getAggregations());
      resultDto = builder.build();
      rval = getResponseStudyIds(resultDto.getAggsList());
    }
    return rval;
  }

  private void appendAggregations(SearchRequestBuilder defaultRequestBuilder, SearchRequestBuilder requestBuilder,
    List<String> aggregationGroupBy) throws IOException {
    aggregationYamlParser.setLocales(micaConfigService.getConfig().getLocales());
    Map<String, Properties> subAggregations = Maps.newHashMap();
    Properties aggregationProperties = getFilteredAggregationsProperties();

    if(aggregationGroupBy != null)
      aggregationGroupBy.forEach(field -> subAggregations.put(field, aggregationProperties));

    aggregationYamlParser.getAggregations(aggregationProperties, subAggregations).forEach(agg -> {
      defaultRequestBuilder.addAggregation(agg);
      requestBuilder.addAggregation(agg);
    });
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
    log.debug("start processAggregations");
    EsQueryResultParser parser = EsQueryResultParser.newParser(aggregationTitleResolver, locale);
    builder.addAllAggs(parser.parseAggregations(defaults, aggregations)); //
    builder.setTotalCount(parser.getTotalCount());
  }

  /**
   * Returns study ID fields (aggregation fields)
   *
   * @return
   */
  protected List<String> getAggJoinFields() {
    return getJoinFields();
  }

  protected abstract List<String> getJoinFields();

  private QueryBuilder newStudyIdQuery(List<String> studyIds) {
    return hasQueryBuilder() ? addStudyIdQuery(studyIds) : createStudyIdQuery(studyIds);
  }

  private QueryBuilder addStudyIdQuery(List<String> studyIds) {
    if(studyIds == null || studyIds.isEmpty()) return queryWrapper.getQueryBuilder();
    return QueryBuilders.boolQuery().must(queryWrapper.getQueryBuilder()).must(createStudyIdQuery(studyIds));
  }

  private QueryBuilder createStudyIdQuery(List<String> studyIds) {
    List<String> joinFields = getJoinFields();
    if(joinFields.size() == 1) return QueryBuilders.termsQuery(joinFields.get(0), studyIds);
    else {
      BoolQueryBuilder builder = QueryBuilders.boolQuery();
      getJoinFields().forEach(field -> builder.should(QueryBuilders.termsQuery(field, studyIds)));
      return builder;
    }
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
    return getDocumentCountsFilteredByClassName(joinField, null);
  }

  protected Map<String, Integer> getDocumentCountsFilteredByClassName(String joinField, String className) {
    if(resultDto == null) return Maps.newHashMap();

    return
      resultDto.getAggsList().stream()
        .filter(agg -> joinField.equals(AggregationAliasHelper.unformatName(agg.getAggregation())))
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)).flatMap(Collection::stream)
        .filter(s -> s.getCount() > 0)
        .filter(s -> Strings.isNullOrEmpty(className) || className.equals(s.getClassName()))
        .collect(Collectors.toMap(
          MicaSearch.TermsAggregationResultDto::getKey,
          MicaSearch.TermsAggregationResultDto::getCount)
      );
  }

  protected Map<String, Integer> getDocumentBucketCounts(String joinField, String bucketField, String bucketValue) {
    if(resultDto == null) return Maps.newHashMap();
    return resultDto.getAggsList().stream() //
        .filter(agg -> bucketField.equals(AggregationAliasHelper.unformatName(agg.getAggregation()))) //
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
        .flatMap(Collection::stream) //
        .filter(t -> bucketValue.equals(t.getKey())) //
        .map(t -> t.getAggsList())
        .flatMap(Collection::stream)
        .filter(agg -> joinField.equals(AggregationAliasHelper.unformatName(agg.getAggregation()))) //
        .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
        .flatMap(Collection::stream) //
        .filter(s -> s.getCount() > 0) //
        .collect(Collectors.toMap(MicaSearch.TermsAggregationResultDto::getKey, term -> term.getCount()));
  }

  protected List<String> getResponseDocumentIds(List<String> fields, List<MicaSearch.AggregationResultDto> aggDtos) {
    log.debug("start getResponseDocumentIds");
    List<String> ids = aggDtos.stream() //
      .filter(agg -> fields.contains(AggregationAliasHelper.unformatName(agg.getAggregation()))) //
      .map(d -> d.getExtension(MicaSearch.TermsAggregationResultDto.terms)) //
      .flatMap((d) -> d.stream()) //
      .filter(s -> s.getCount() > 0) //
      .map(MicaSearch.TermsAggregationResultDto::getKey) //
      .collect(Collectors.toList()); //
    return ids;
  }

}
