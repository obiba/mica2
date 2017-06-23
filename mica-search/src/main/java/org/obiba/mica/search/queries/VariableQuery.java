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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.search.VariableIndexer;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.DocumentQueryHelper;
import org.obiba.mica.search.DocumentQueryIdProvider;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.DataCollectionEventAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.DatasetAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.StudyAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.TaxonomyAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.VariableTaxonomyMetaDataProvider;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import sun.util.locale.LanguageTag;

import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.DETAIL;
import static org.obiba.mica.search.queries.AbstractDocumentQuery.Scope.NONE;

@Component
@Scope("request")
public class VariableQuery extends AbstractDocumentQuery {

  private static final String JOIN_FIELD = "studyIds";

  private static final String DATASET_ID = "datasetId";

  private static final String NETWORK_ID = "networkId";

  private static final String VARIABLE_TYPE = "variableType";

  @Inject
  private OpalService opalService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private Dtos dtos;

  @Inject
  private DatasetAggregationMetaDataProvider datasetAggregationMetaDataProvider;

  @Inject
  private TaxonomyAggregationMetaDataProvider taxonomyAggregationMetaDataProvider;

  @Inject
  private VariableTaxonomyMetaDataProvider variableTaxonomyMetaDataProvider;

  @Inject
  private DataCollectionEventAggregationMetaDataProvider dceAggregationMetaDataProvider;

  @Inject
  private StudyAggregationMetaDataProvider studyAggregationMetaDataProvider;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private CollectionDatasetService collectionDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  private DocumentQueryIdProvider datasetIdProvider;

  @Override
  public String getSearchIndex() {
    return VariableIndexer.PUBLISHED_VARIABLE_INDEX;
  }

  @Override
  public String getSearchType() {
    return VariableIndexer.VARIABLE_TYPE;
  }

  @Override
  public QueryBuilder getAccessFilter() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = collectionDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/collection-dataset", s)).collect(Collectors.toList());
    ids.addAll(harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList()));

    if(ids.isEmpty()) return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"));

    BoolQueryBuilder orFilter = QueryBuilders.boolQuery();
    ids.forEach(id -> orFilter.should(QueryBuilders.termQuery("datasetId", id)));

    return orFilter;
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(VariableIndexer.LOCALIZED_ANALYZED_FIELDS).map(f -> "attributes." + f);
  }

  @Override
  public Stream<String> getQueryStringFields() {
    return Stream.of(VariableIndexer.ANALYZED_FIELDS);
  }

  public void setDatasetIdProvider(DocumentQueryIdProvider provider) {
    datasetIdProvider = provider;
  }

  @Override
  protected void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits, Scope scope,
    CountStatsData counts) throws IOException {
    MicaSearch.DatasetVariableResultDto.Builder resBuilder = MicaSearch.DatasetVariableResultDto.newBuilder();
    Map<String, BaseStudy> studyMap = Maps.newHashMap();
    Map<String, Network> networkMap = Maps.newHashMap();

    for(SearchHit hit : hits) {
      DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(hit.getId());
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      DatasetVariable variable = objectMapper.readValue(inputStream, DatasetVariable.class);
      resBuilder.addSummaries(processHit(resolver, variable, studyMap, networkMap));
    }

    builder.setExtension(MicaSearch.DatasetVariableResultDto.result, resBuilder.build());
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays
      .asList(taxonomyAggregationMetaDataProvider, variableTaxonomyMetaDataProvider, datasetAggregationMetaDataProvider,
        dceAggregationMetaDataProvider, studyAggregationMetaDataProvider);
  }

  @Override
  protected boolean ignoreFields() {
    // always needs actual documents
    return false;
  }

  /**
   * Fill the variable resolver dto with user-friendly labels about the variable, the dataset and the study.
   *
   * @param resolver
   * @param variable
   * @param studyMap study cache
   * @return
   */
  private Mica.DatasetVariableResolverDto processHit(DatasetVariable.IdResolver resolver, DatasetVariable variable,
    Map<String, BaseStudy> studyMap, Map<String, Network> networkMap) {
    Mica.DatasetVariableResolverDto.Builder builder = dtos.asDto(resolver);

    String studyId = resolver.hasStudyId() ? resolver.getStudyId() : null;

    if(resolver.getType() == DatasetVariable.Type.Collection || resolver.getType() == DatasetVariable.Type.Harmonized) {
      studyId = variable.getStudyIds().get(0);
    }

    String networkId = variable.getNetworkId();

    if (networkId != null) { // harmonized
      Network network;

      if(networkMap.containsKey(networkId)) network = networkMap.get(networkId);
      else {
        network = publishedNetworkService.findById(networkId);
        networkMap.put(networkId, network);
      }

      if(network != null) {
        builder.setNetworkId(networkId);
        builder.addAllNetworkName(dtos.asDto(network.getName()));
        builder.addAllNetworkAcronym(dtos.asDto(network.getAcronym()));
      }
    }

    if(studyId != null) {
      builder.setStudyId(studyId);

      try {
        BaseStudy study;
        if(studyMap.containsKey(studyId)) {
          study = studyMap.get(studyId);
        } else {
          study = publishedStudyService.findById(studyId);
          studyMap.put(studyId, study);
        }

        if(study != null) {
          builder.addAllStudyName(dtos.asDto(study.getName()));
          builder.addAllStudyAcronym(dtos.asDto(study.getAcronym()));
        }
      } catch(NoSuchStudyException e) {
      }
    }

    builder.addAllDatasetAcronym(dtos.asDto(variable.getDatasetAcronym()));
    builder.addAllDatasetName(dtos.asDto(variable.getDatasetName()));

    if(variable.hasAttribute("label", null)) {
      builder.addAllVariableLabel(dtos.asDto(variable.getAttributes().getAttribute("label", null).getValues()));
    }

    return builder.build();
  }

  @Override
  protected QueryBuilder updateJoinKeyQuery(QueryBuilder queryBuilder) {
    return DocumentQueryHelper.updateDatasetJoinKeysQuery(queryBuilder, DATASET_ID, datasetIdProvider);
  }

  @Override
  protected DocumentQueryJoinKeys processJoinKeys(SearchResponse response) {
    return DocumentQueryHelper.processDatasetJoinKeys(response, DATASET_ID, datasetIdProvider);
  }

  @Override
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    updateDatasetQuery();
    List<String> ids = super.query(studyIds, null, scope == DETAIL ? DETAIL : NONE);
    if(datasetIdProvider != null) datasetIdProvider.setIds(getDatasetIds());
    return ids;
  }

  private void updateDatasetQuery() {
    if(datasetIdProvider == null) return;
    List<String> datasetIds = datasetIdProvider.getIds();
    if(datasetIds.size() > 0) {
      TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery(DATASET_ID, datasetIds);
      setQueryBuilder(hasQueryBuilder()
        ? QueryBuilders.boolQuery().must(getQueryBuilder()).must(termsQueryBuilder)
        : termsQueryBuilder);
    }
  }

  public Map<String, Integer> getDatasetCounts() {
    return getDocumentCounts(DATASET_ID);
  }

  public Map<String, Integer> getStudyVariableByDatasetCounts() {
    return getDocumentBucketCounts(DATASET_ID, VARIABLE_TYPE, DatasetVariable.Type.Collection.name());
  }

  public Map<String, Integer> getDataschemaVariableByDatasetCounts() {
    return getDocumentBucketCounts(DATASET_ID, VARIABLE_TYPE, DatasetVariable.Type.Dataschema.name());
  }

  private List<String> getDatasetIds() {
    if(resultDto != null) {
      return getResponseDocumentIds(Arrays.asList(DATASET_ID), resultDto.getAggsList());
    }

    return Lists.newArrayList();
  }

  @Nullable
  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

  public Map<String, Integer> getNetworkCounts() {
    return getDocumentCounts(NETWORK_ID);
  }

  public Map<String, Integer> getStudyVariableByStudyCounts() {
    return getDocumentBucketCounts(JOIN_FIELD, VARIABLE_TYPE, "Study");
  }

  public Map<String, Integer> getDataschemaVariableByStudyCounts() {
    return getDocumentBucketCounts(JOIN_FIELD, VARIABLE_TYPE, "Dataschema");
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD, DATASET_ID);
  }

  @NotNull
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = new Properties();
    if(mode != Mode.LIST && filter != null) {
      List<Pattern> patterns = filter.stream().map(Pattern::compile).collect(Collectors.toList());

      getOpalTaxonomies().stream().filter(Taxonomy::hasVocabularies)
        .forEach(taxonomy -> taxonomy.getVocabularies().stream().filter(Vocabulary::hasTerms).forEach(vocabulary -> {
          String field = vocabulary.getAttributes().containsKey("field")
            ? vocabulary.getAttributeValue("field")
            : "attributes." + AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()) + "." +
              LanguageTag.UNDETERMINED;
          if(patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(field).find()))
            properties.put(field, "");
        }));

      taxonomyService.getVariableTaxonomy().getVocabularies().forEach(vocabulary -> {
        String field = vocabulary.getAttributes().containsKey("field")
          ? vocabulary.getAttributeValue("field")
          : vocabulary.getName().replace('-', '.');
        if(patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(field).matches()))
          properties.put(field, "");
      });
    }

    // required for the counts to work
    if(!properties.containsKey(JOIN_FIELD)) properties.put(JOIN_FIELD, "");
    if(!properties.containsKey(DATASET_ID)) properties.put(DATASET_ID, "");
    if(!properties.containsKey(NETWORK_ID)) properties.put(NETWORK_ID, "");

    return properties;
  }

  @Override
  protected List<String> getAggregationGroupBy() {
    List<String> buckets = super.getAggregationGroupBy();
    // always group by variable type
    if (!buckets.contains(VARIABLE_TYPE)) {
      return ImmutableList.<String>builder().addAll(buckets).add(VARIABLE_TYPE).build();
    }
    return buckets;
  }

  @NotNull
  private List<Taxonomy> getOpalTaxonomies() {
    List<Taxonomy> taxonomies = null;

    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }

    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }
}
