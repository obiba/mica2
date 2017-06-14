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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.micaConfig.service.helper.AggregationAliasHelper;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.DocumentQueryHelper;
import org.obiba.mica.search.DocumentQueryIdProvider;
import org.obiba.mica.search.aggregations.DatasetTaxonomyMetaDataProvider;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.obiba.mica.search.CountStatsDtoBuilders.DatasetCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.DatasetResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class DatasetQuery extends AbstractDocumentQuery {

  private static final Logger log = LoggerFactory.getLogger(DatasetQuery.class);

  public static final String ID  = "id";

  public static final String STUDY_JOIN_FIELD = "studyTable.studyId";

  public static final String HARMONIZATION_JOIN_FIELD = "studyTables.studyId";

  public static final String NETWORK_JOIN_FIELD = "networkId";

  private enum DatasetType {
    STUDY,
    HARMONIZATION,
    DATASET
  }

  @Inject
  Dtos dtos;

  @Inject
  PublishedDatasetService publishedDatasetService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private DatasetTaxonomyMetaDataProvider datasetTaxonomyMetaDataProvider;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  private DocumentQueryIdProvider datasetIdProvider;

  @Override
  public String getSearchIndex() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  public String getSearchType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  @Override
  public QueryBuilder getAccessFilter() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    ids.addAll(harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList()));
    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(DatasetIndexer.LOCALIZED_ANALYZED_FIELDS);
  }

  public void setDatasetIdProvider(DocumentQueryIdProvider provider) {
    datasetIdProvider = provider;
  }


  @Override
  protected QueryBuilder updateJoinKeyQuery(QueryBuilder queryBuilder) {
    return DocumentQueryHelper.updateDatasetJoinKeysQuery(queryBuilder, ID, datasetIdProvider);
  }

  @Override
  protected DocumentQueryJoinKeys processJoinKeys(SearchResponse response) {
    return DocumentQueryHelper.processDatasetJoinKeys(response, ID, datasetIdProvider);
  }

  @Override
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    updateDatasetQuery();
    List<String> ids = super.query(studyIds, counts, scope);
    if(datasetIdProvider != null) datasetIdProvider.setIds(getDatasetIds());
    return ids;
  }

  @Nullable
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = getAggregationsProperties(filter, taxonomyService.getDatasetTaxonomy());
    if(!properties.containsKey(STUDY_JOIN_FIELD)) properties.put(STUDY_JOIN_FIELD, "");
    if(!properties.containsKey(HARMONIZATION_JOIN_FIELD)) properties.put(HARMONIZATION_JOIN_FIELD, "");
    if(!properties.containsKey(NETWORK_JOIN_FIELD)) properties.put(NETWORK_JOIN_FIELD, "");
    if(!properties.containsKey(ID)) properties.put(ID,"");
    return properties;
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope, CountStatsData counts) {
    DatasetResultDto.Builder resBuilder = DatasetResultDto.newBuilder();
    DatasetCountStatsBuilder datasetCountStatsBuilder = counts == null
      ? null
      : DatasetCountStatsBuilder.newBuilder(counts);

    Consumer<Dataset> addDto = getDatasetConsumer(scope, resBuilder, datasetCountStatsBuilder);
    List<Dataset> datasets = publishedDatasetService
      .findByIds(Stream.of(hits.hits()).map(h -> h.getId()).collect(Collectors.toList()));
    datasets.forEach(addDto);
    builder.setExtension(DatasetResultDto.result, resBuilder.build());
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(datasetTaxonomyMetaDataProvider);
  }

  private List<String> getDatasetIds() {
    if(resultDto != null) {
      return getResponseDocumentIds(Arrays.asList("id"), resultDto.getAggsList());
    }

    return Lists.newArrayList();
  }

  private void updateDatasetQuery() {
    if(datasetIdProvider == null) return;
    List<String> datasetIds = datasetIdProvider.getIds();
    if(datasetIds.size() > 0) {
      TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("id", datasetIds);
      setQueryBuilder(hasQueryBuilder()
        ? QueryBuilders.boolQuery().must(getQueryBuilder()).must(termsQueryBuilder)
        : termsQueryBuilder);
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
    return Arrays.asList(STUDY_JOIN_FIELD, HARMONIZATION_JOIN_FIELD, ID);
  }

  public Map<String, Map<String, List<String>>> getStudyCountsByDataset() {
    QueryBuilder accessFilter = getAccessFilter();
    QueryBuilder query = hasQueryBuilder() ? getQueryBuilder() : QueryBuilders.matchAllQuery();

    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSize(0) //
      .setQuery(accessFilter == null ? query : QueryBuilders.boolQuery().must(query).must(accessFilter)) //
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
        .forEach(bucket -> map.put(bucket.getKeyAsString(), getStudyCounts(bucket.getAggregations())));
    } catch(IndexNotFoundException e) {
      // ignore
    }

    return map;
  }

  private Map<String, List<String>> getStudyCounts(Aggregations aggregations) {
    Map<String, List<String>> map = Maps.newHashMap();
    aggregations.forEach(aggregation -> map.put(AggregationAliasHelper.unformatName(aggregation.getName()),
      ((Terms) aggregation).getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
        .map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList())));

    return map;
  }

  public Map<String, Integer> getNetworkCounts() {
    return getDocumentCounts(NETWORK_JOIN_FIELD);
  }

  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(STUDY_JOIN_FIELD);
  }

  public Map<String, Integer> getHarmonizationStudyCounts() {
    return getDocumentCounts(HARMONIZATION_JOIN_FIELD);
  }

}
