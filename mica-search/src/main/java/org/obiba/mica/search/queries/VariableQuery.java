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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
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
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.DatasetIdProvider;
import org.obiba.mica.search.aggregations.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.DataCollectionEventAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.DatasetAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.StudyAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.TaxonomyAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.VariableTaxonomyMetaDataProvider;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  @Inject
  private OpalService opalService;

  @Inject
  private PublishedStudyService publishedStudyService;

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
  StudyAggregationMetaDataProvider studyAggregationMetaDataProvider;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  private DatasetIdProvider datasetIdProvider;

  private Collection<String> taxonomyFilter = Collections.emptyList();

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
    List<String> ids = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    ids.addAll(harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList()));

    if(ids.isEmpty()) return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"));

    BoolQueryBuilder orFilter = QueryBuilders.boolQuery();
    ids.stream().forEach(id -> orFilter.should(QueryBuilders.termQuery("datasetId", id)));

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

  public void setDatasetIdProvider(DatasetIdProvider provider) {
    datasetIdProvider = provider;
  }

  @Override
  protected void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits, Scope scope,
    CountStatsData counts) throws IOException {
    MicaSearch.DatasetVariableResultDto.Builder resBuilder = MicaSearch.DatasetVariableResultDto.newBuilder();
    Map<String, Study> studyMap = Maps.newHashMap();

    for(SearchHit hit : hits) {
      DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(hit.getId());
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      DatasetVariable variable = objectMapper.readValue(inputStream, DatasetVariable.class);
      resBuilder.addSummaries(processHit(resolver, variable, studyMap));
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
    Map<String, Study> studyMap) {
    Mica.DatasetVariableResolverDto.Builder builder = dtos.asDto(resolver);

    String studyId = resolver.hasStudyId() ? resolver.getStudyId() : null;

    if(resolver.getType() == DatasetVariable.Type.Study || resolver.getType() == DatasetVariable.Type.Harmonized) {
      studyId = variable.getStudyIds().get(0);
    }

    if(studyId != null) {
      builder.setStudyId(studyId);

      try {
        Study study;
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
  public List<String> query(List<String> studyIds, CountStatsData counts, Scope scope) throws IOException {
    updateDatasetQuery();
    List<String> ids = super.query(studyIds, null, scope == DETAIL ? DETAIL : NONE);
    if(datasetIdProvider != null & hasQueryBuilder()) datasetIdProvider.setDatasetIds(getDatasetIds());
    return ids;
  }

  private void updateDatasetQuery() {
    if(datasetIdProvider == null) return;
    List<String> datasetIds = datasetIdProvider.getDatasetIds();
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

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  @NotNull
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    if(filter == null) return null;

    Properties properties = new Properties();
    if(mode != Mode.LIST) {
      List<Pattern> patterns = filter.stream().map(Pattern::compile).collect(Collectors.toList());

      getOpalTaxonomies().stream().filter(Taxonomy::hasVocabularies)
        .forEach(taxonomy -> taxonomy.getVocabularies().stream().filter(Vocabulary::hasTerms).forEach(vocabulary -> {
          String key = "attributes." + AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()) + "." +
            LanguageTag.UNDETERMINED;
          if(patterns.isEmpty() || patterns.stream().filter(p -> p.matcher(key).matches()).findFirst().isPresent())
            properties.put(key, "");
        }));

      micaConfigService.getVariableTaxonomy().getVocabularies().forEach(vocabulary -> {
        String key = vocabulary.getName().replace('-','.');
        if(patterns.isEmpty() || patterns.stream().filter(p -> p.matcher(key).matches()).findFirst().isPresent())
          properties.put(key,"");
      });
    }
    return properties;
  }

  @NotNull
  private List<Taxonomy> getOpalTaxonomies() {
    List<Taxonomy> taxonomies = null;

    try {
      taxonomies = opalService.getTaxonomies();
      if(!taxonomyFilter.isEmpty())
        taxonomies = taxonomies.stream().filter(t -> taxonomyFilter.contains(t.getName())).collect(Collectors.toList());
    } catch(Exception e) {
      // ignore
    }

    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  public void setTaxonomyFilter(Collection<String> taxonomyFilter) {
    this.taxonomyFilter = taxonomyFilter == null ? Collections.emptyList() : taxonomyFilter;
  }
}
