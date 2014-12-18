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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.DatasetIdProvider;
import org.obiba.mica.search.aggregations.AggregationYamlParser;
import org.obiba.mica.search.rest.QueryDtoHelper;
import org.obiba.mica.search.rest.QueryDtoParser;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.obiba.mica.search.CountStatsDtoBuilders.DatasetCountStatsBuilder;
import static org.obiba.mica.search.rest.QueryDtoHelper.BoolQueryType;
import static org.obiba.mica.web.model.MicaSearch.DatasetResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class DatasetQuery extends AbstractDocumentQuery {

  private static final Logger log = LoggerFactory.getLogger(DatasetQuery.class);

  private static final String DATASET_FACETS_YML = "dataset-facets.yml";

  public static final String STUDY_JOIN_FIELD = "studyTable.studyId";

  public static final String HARMONIZATION_JOIN_FIELD = "studyTables.studyId";

  private static final String CLASS_NAME_AGG = "className";

  private enum DatasetType {
    STUDY,
    HARMONIZATION,
    DATASET
  }

  @Inject
  Dtos dtos;

  @Inject
  PublishedDatasetService publishedDatasetService;

  private DatasetIdProvider datasetIdProvider;

  @Override
  public String getSearchIndex() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  public String getSearchType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(DatasetIndexer.LOCALIZED_ANALYZED_FIELDS);
  }

  public void setDatasetIdProvider(DatasetIdProvider provider) {
    datasetIdProvider = provider;
  }

  @Override
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    updateDatasetQuery();
    List<String> ids = super.query(studyIds, counts, scope);
    if(datasetIdProvider != null) datasetIdProvider.setDatasetIds(getDatasetIds());
    return ids;
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(DATASET_FACETS_YML);
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope, CountStatsData counts) {
    DatasetResultDto.Builder resBuilder = DatasetResultDto.newBuilder();
    DatasetCountStatsBuilder datasetCountStatsBuilder = counts == null
      ? null
      : DatasetCountStatsBuilder.newBuilder(counts);

    Consumer<Dataset> addDto = getDatasetConsumer(scope, resBuilder, datasetCountStatsBuilder);

    for(SearchHit hit : hits) {
      addDto.accept(publishedDatasetService.findById(hit.getId()));
    }

    builder.setExtension(DatasetResultDto.result, resBuilder.build());
  }

  private List<String> getDatasetIds() {
    if(resultDto != null) {
      return getResponseDocumentIds(Arrays.asList("id"), resultDto.getAggsList());
    }

    return Lists.newArrayList();
  }

  private void updateDatasetQuery() {
    if(datasetIdProvider == null) return;
    List<String> datasetIds = datasetIdProvider.getDatasetIds();
    if(datasetIds.size() > 0) {
      if(queryDto == null) {
        queryDto = QueryDtoHelper.createTermFiltersQuery(Arrays.asList("id"), datasetIds, BoolQueryType.MUST);
      } else {
        queryDto = QueryDtoHelper
          .addTermFilters(queryDto, QueryDtoHelper.createTermFilters(Arrays.asList("id"), datasetIds),
            BoolQueryType.MUST);
      }
    }
  }

  private Consumer<Dataset> getDatasetConsumer(Scope scope, DatasetResultDto.Builder resBuilder,
    DatasetCountStatsBuilder datasetCountStatsBuilder) {

    return scope == Scope.DETAIL ? (dataset) -> {
      Mica.DatasetDto.Builder datasetBuilder = dtos.asDtoBuilder(dataset);
      if(datasetCountStatsBuilder != null) {
        datasetBuilder.setExtension(MicaSearch.CountStatsDto.datasetCountStats, datasetCountStatsBuilder.build(dataset))
          .build();
      }
      resBuilder.addDatasets(datasetBuilder.build());
    } : (dataset) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(dataset).build());
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(STUDY_JOIN_FIELD, HARMONIZATION_JOIN_FIELD);
  }

  protected MicaSearch.QueryDto addStudyIdFilters(List<String> studyIds) {
    if((datasetIdProvider != null && datasetIdProvider.getDatasetIds().size() > 0) || studyIds == null ||
      studyIds.size() == 0) {
      return queryDto;
    }

    List<String> joinFields = getJoinFieldsByType(findType());
    BoolQueryType operator = joinFields.size() == 1 ? BoolQueryType.MUST : BoolQueryType.SHOULD;
    return QueryDtoHelper.addTermFilters(MicaSearch.QueryDto.newBuilder(queryDto).build(),
      QueryDtoHelper.createTermFilters(joinFields, studyIds), operator);
  }

  protected MicaSearch.QueryDto createStudyIdFilters(List<String> studyIds) {
    List<String> joinFields = getJoinFieldsByType(findType());
    BoolQueryType operator = joinFields.size() == 1 ? BoolQueryType.MUST : BoolQueryType.SHOULD;
    return QueryDtoHelper.createTermFiltersQuery(joinFields, studyIds, operator);
  }

  private List<String> getJoinFieldsByType(DatasetType type) {
    switch(type) {
      case STUDY:
        return Arrays.asList(STUDY_JOIN_FIELD);
      case HARMONIZATION:
        return Arrays.asList(HARMONIZATION_JOIN_FIELD);
      case DATASET:
        return getJoinFields();
    }

    throw new IllegalArgumentException("Invaid Dataset type: " + type.name());
  }

  protected DatasetType findType() {
    if(queryDto == null) return DatasetType.DATASET;
    QueryDtoParser queryDtoParser = QueryDtoParser.newParser();
    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
      .setQuery(queryDtoParser.parse(queryDto)) //
      .setNoFields();

    Properties classNameAggregation = new Properties();
    classNameAggregation.setProperty(CLASS_NAME_AGG, "");

    try {
      aggregationYamlParser.getAggregations(classNameAggregation).forEach(requestBuilder::addAggregation);
    } catch(IOException e) {
      log.error("Failed to add aggregation for finding Dataset types: '{}'", e);
      return DatasetType.DATASET;
    }

    log.info("Request: {}", requestBuilder);
    SearchResponse response = requestBuilder.execute().actionGet();
    List<String> classNames = Lists.newArrayList();

    response.getAggregations().forEach(aggregation -> ((Terms) aggregation).getBuckets().stream().forEach(bucket -> {
      if(bucket.getDocCount() > 0) classNames.add(bucket.getKey());
    }));

    int count = classNames.size();

    if(count == 1) {
      return classNames.get(0).equals(HarmonizationDataset.class.getSimpleName().toLowerCase())
        ? DatasetType.HARMONIZATION
        : DatasetType.STUDY;
    }

    return DatasetType.DATASET;
  }

  public Map<String, Map<String, List<String>>> getStudyCountsByDataset() {
    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
      .setQuery(queryDto == null ? QueryBuilders.matchAllQuery() : QueryDtoParser.newParser().parse(queryDto)) //
      .setNoFields();

    Properties props = new Properties();
    props.setProperty("id", "");
    Properties subProps = new Properties();
    getJoinFields().forEach(joinField -> subProps.setProperty(joinField, ""));
    Map<String, Properties> subAggregations = Maps.newHashMap();
    subAggregations.put("id", subProps);
    try {
      aggregationYamlParser.getAggregations(props, subAggregations).forEach(requestBuilder::addAggregation);
    } catch(IOException e) {
      log.error("Failed to add Study By dataset aggregations");
      return Maps.newHashMap();
    }

    Map<String, Map<String, List<String>>> map = Maps.newHashMap();
    try {
      SearchResponse response = requestBuilder.execute().actionGet();

      Aggregation idAgg = response.getAggregations().get("id");

      ((Terms) idAgg).getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
        .forEach(bucket -> map.put(bucket.getKey(), getStudyCounts(bucket.getAggregations())));
    } catch(IndexMissingException e) {
      // ignore
    }

    return map;
  }

  private Map<String, List<String>> getStudyCounts(Aggregations aggregations) {
    Map<String, List<String>> map = Maps.newHashMap();
    aggregations.forEach(aggregation -> map.put(AggregationYamlParser.unformatName(aggregation.getName()),
      ((Terms) aggregation).getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
        .map(bucket -> bucket.getKey()).collect(Collectors.toList())));

    return map;
  }

  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(STUDY_JOIN_FIELD);
  }

  public Map<String, Integer> getHarmonizationStudyCounts() {
    return getDocumentCounts(HARMONIZATION_JOIN_FIELD);
  }

}
