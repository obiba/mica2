/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.search.DocumentQueryHelper;
import org.obiba.mica.search.DocumentQueryIdProvider;
import org.obiba.mica.search.aggregations.*;
import org.obiba.mica.spi.search.*;
import org.obiba.mica.spi.search.support.AttributeKey;
import org.obiba.mica.spi.search.support.Query;
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
import sun.util.locale.LanguageTag;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.obiba.mica.spi.search.QueryScope.DETAIL;
import static org.obiba.mica.spi.search.QueryScope.NONE;

@Component
@Scope("request")
public class VariableQuery extends AbstractDocumentQuery {

  private static final String JOIN_FIELD = "studyId";

  private static final String DATASET_ID = "datasetId";

  private static final String VARIABLE_TYPE = "variableType";

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
  private StudyAggregationMetaDataProvider studyAggregationMetaDataProvider;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  private DocumentQueryIdProvider datasetIdProvider;

  @Override
  public String getSearchIndex() {
    return Indexer.PUBLISHED_VARIABLE_INDEX;
  }

  @Override
  public String getSearchType() {
    return Indexer.VARIABLE_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {

      @Override
      public String getField() {
        return "datasetId";
      }

      @Override
      public Collection<String> getValues() {
        List<String> ids = collectedDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
            .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
        ids.addAll(harmonizedDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
            .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList()));
        return ids;
      }
    };
  }

  public void setDatasetIdProvider(DocumentQueryIdProvider provider) {
    datasetIdProvider = provider;
  }

  @Override
  protected void processHits(MicaSearch.QueryResultDto.Builder builder, Searcher.DocumentResults results, QueryScope scope,
                             CountStatsData counts) throws IOException {
    MicaSearch.DatasetVariableResultDto.Builder resBuilder = MicaSearch.DatasetVariableResultDto.newBuilder();
    Map<String, BaseStudy> studyMap = Maps.newHashMap();
    Map<String, Network> networkMap = Maps.newHashMap();

    for (Searcher.DocumentResult result : results.getDocuments()) {
      if (result.hasSource()) {
        DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(result.getId());
        DatasetVariable variable = objectMapper.readValue(result.getSourceInputStream(), DatasetVariable.class);
        resBuilder.addSummaries(processHit(resolver, variable, studyMap, networkMap));
      }
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
  protected List<String> getMandatorySourceFields() {
    return Lists.newArrayList(
        "id",
        "studyId"
    );
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

    if (resolver.getType() == DatasetVariable.Type.Collected || resolver.getType() == DatasetVariable.Type.Dataschema || resolver.getType() == DatasetVariable.Type.Harmonized) {
      studyId = variable.getStudyId();
    }

    if (studyId != null) {
      builder.setStudyId(studyId);

      try {
        BaseStudy study;
        if (studyMap.containsKey(studyId)) {
          study = studyMap.get(studyId);
        } else {
          study = publishedStudyService.findById(studyId);
          studyMap.put(studyId, study);
        }

        if (study != null) {
          builder.addAllStudyName(dtos.asDto(study.getName()));
          builder.addAllStudyAcronym(dtos.asDto(study.getAcronym()));
        }
      } catch (NoSuchStudyException e) {
      }
    }

    builder.addAllDatasetAcronym(dtos.asDto(variable.getDatasetAcronym()));
    builder.addAllDatasetName(dtos.asDto(variable.getDatasetName()));

    if (variable.hasAttribute("label", null)) {
      builder.addAllVariableLabel(dtos.asDto(variable.getAttributes().getAttribute("label", null).getValues()));
    }

    return builder.build();
  }

  @Override
  protected Query updateWithJoinKeyQuery(Query query) {
    return searcher.andQuery(query, searcher.makeQuery(DocumentQueryHelper.inRQL(DATASET_ID, datasetIdProvider)));
  }

  @Override
  protected DocumentQueryJoinKeys processJoinKeys(Searcher.DocumentResults results) {
    return DocumentQueryHelper.processDatasetJoinKeys(results, DATASET_ID, datasetIdProvider);
  }

  @Override
  public void query(List<String> studyIds, CountStatsData counts, QueryScope scope) {
    updateDatasetQuery();
    super.query(studyIds, null, scope == DETAIL ? DETAIL : NONE);
    if (datasetIdProvider != null) datasetIdProvider.setIds(getDatasetIds());
  }

  private void updateDatasetQuery() {
    if (datasetIdProvider == null) return;
    List<String> datasetIds = datasetIdProvider.getIds();
    if (datasetIds.size() > 0) {
      Query datasetQuery = searcher.makeQuery(String.format("in(%s,(%s))", DATASET_ID, Joiner.on(",").join(datasetIds)));
      setQuery(isQueryNotEmpty() ? searcher.andQuery(getQuery(), datasetQuery) : datasetQuery);
    }
  }

  public Map<String, Integer> getDatasetCounts() {
    return getDocumentCounts(DATASET_ID);
  }

  public Map<String, Integer> getStudyVariableByDatasetCounts() {
    return getDocumentBucketCounts(DATASET_ID, VARIABLE_TYPE, DatasetVariable.Type.Collected.name());
  }

  public Map<String, Integer> getDataschemaVariableByDatasetCounts() {
    return getDocumentBucketCounts(DATASET_ID, VARIABLE_TYPE, DatasetVariable.Type.Dataschema.name());
  }

  private List<String> getDatasetIds() {
    if (resultDto != null) {
      return getResponseDocumentIds(Arrays.asList(DATASET_ID), resultDto.getAggsList());
    }

    return Lists.newArrayList();
  }

  @Nullable
  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

  public Map<String, Integer> getStudyVariableByStudyCounts() {
    return getDocumentBucketCounts(JOIN_FIELD, VARIABLE_TYPE, DatasetVariable.Type.Collected.name());
  }

  public Map<String, Integer> getDataschemaVariableByStudyCounts() {
    return getDocumentBucketCounts(JOIN_FIELD, VARIABLE_TYPE, DatasetVariable.Type.Dataschema.name());
  }

  @Override
  protected List<String> getAggJoinFields() {
    return Arrays.asList(JOIN_FIELD, DATASET_ID);
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  @NotNull
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = new Properties();
    if (mode != QueryMode.LIST && filter != null && !filter.isEmpty()) {
      List<Pattern> patterns = filter.stream().map(Pattern::compile).collect(Collectors.toList());

      getOpalTaxonomies().stream().filter(Taxonomy::hasVocabularies)
          .forEach(taxonomy -> taxonomy.getVocabularies().stream().filter(Vocabulary::hasTerms).forEach(vocabulary -> {
            String field = vocabulary.getAttributes().containsKey("field")
                ? vocabulary.getAttributeValue("field")
                : "attributes." + AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()) + "." +
                LanguageTag.UNDETERMINED;
            if (patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(field).find()))
              properties.put(field, "");
          }));

      taxonomyService.getVariableTaxonomy().getVocabularies().forEach(vocabulary -> {
        String field = vocabulary.getAttributes().containsKey("field")
            ? vocabulary.getAttributeValue("field")
            : vocabulary.getName().replace('-', '.');
        if (patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(field).matches()))
          properties.put(field, "");
      });
    }

    // required for the counts to work
    if (!properties.containsKey(JOIN_FIELD)) properties.put(JOIN_FIELD, "");
    if (!properties.containsKey(DATASET_ID)) properties.put(DATASET_ID, "");

    return properties;
  }

  @Override
  protected List<String> getAdditionalAggregationBuckets() {
    // always group by variable type
    return Lists.newArrayList(VARIABLE_TYPE);
  }

  @NotNull
  private List<Taxonomy> getOpalTaxonomies() {
    List<Taxonomy> taxonomies = null;

    try {
      taxonomies = opalService.getTaxonomies();
    } catch (Exception e) {
      // ignore
    }

    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }
}
