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
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.DocumentQueryHelper;
import org.obiba.mica.search.DocumentQueryIdProvider;
import org.obiba.mica.search.aggregations.DatasetTaxonomyMetaDataProvider;
import org.obiba.mica.spi.search.CountStatsData;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.AggregationHelper;
import org.obiba.mica.spi.search.support.Query;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.obiba.mica.search.CountStatsDtoBuilders.DatasetCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.DatasetResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class DatasetQuery extends AbstractDocumentQuery {

  private static final Logger log = LoggerFactory.getLogger(DatasetQuery.class);

  public static final String ID = "id";

  public static final String STUDY_JOIN_FIELD = "studyTable.studyId";

  public static final String HARMONIZATION_STUDY_JOIN_FIELD = "harmonizationTable.studyId";

  @Inject
  Dtos dtos;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private DatasetTaxonomyMetaDataProvider datasetTaxonomyMetaDataProvider;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  private DocumentQueryIdProvider datasetIdProvider;

  @Override
  public String getSearchIndex() {
    return Indexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  public String getSearchType() {
    return Indexer.DATASET_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        List<String> ids = collectedDatasetService.findPublishedIds().stream()
            .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
        ids.addAll(harmonizedDatasetService.findPublishedIds().stream()
            .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList()));
        return ids;
      }
    };
  }

  @Override
  protected Taxonomy getTaxonomy() {
    return taxonomyService.getDatasetTaxonomy();
  }

  public void setDatasetIdProvider(DocumentQueryIdProvider provider) {
    datasetIdProvider = provider;
  }


  @Override
  protected Query updateWithJoinKeyQuery(Query query) {
    return searcher.andQuery(query, searcher.makeQuery(DocumentQueryHelper.inRQL(ID, datasetIdProvider)));
  }

  @Override
  protected List<String> getMandatorySourceFields() {
    return Lists.newArrayList(
        "id",
        "className"
    );
  }

  @Override
  protected DocumentQueryJoinKeys processJoinKeys(Searcher.DocumentResults results) {
    return DocumentQueryHelper.processDatasetJoinKeys(results, ID, datasetIdProvider);
  }

  @Override
  public void query(List<String> studyIds, CountStatsData counts, QueryScope scope) {
    updateDatasetQuery();
    super.query(studyIds, counts, scope);
    if (datasetIdProvider != null) datasetIdProvider.setIds(getDatasetIds());
  }

  @Nullable
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = getAggregationsProperties(filter, taxonomyService.getDatasetTaxonomy());
    if (!properties.containsKey(STUDY_JOIN_FIELD)) properties.put(STUDY_JOIN_FIELD, "");
    if (!properties.containsKey(HARMONIZATION_STUDY_JOIN_FIELD)) properties.put(HARMONIZATION_STUDY_JOIN_FIELD, "");
    if (!properties.containsKey(ID)) properties.put(ID, "");
    return properties;
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, Searcher.DocumentResults results, QueryScope scope, CountStatsData counts) {
    DatasetResultDto.Builder resBuilder = DatasetResultDto.newBuilder();
    DatasetCountStatsBuilder datasetCountStatsBuilder = counts == null
        ? null
        : DatasetCountStatsBuilder.newBuilder(counts);

    Consumer<Dataset> addDto = getDatasetConsumer(scope, resBuilder, datasetCountStatsBuilder);
    List<Dataset> published = getPublishedDocumentsFromHitsByClassName(results, Dataset.class);
    published.forEach(addDto::accept);
    builder.setExtension(DatasetResultDto.result, resBuilder.build());
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(datasetTaxonomyMetaDataProvider);
  }

  private List<String> getDatasetIds() {
    if (resultDto != null) {
      return getResponseDocumentIds(Arrays.asList("id"), resultDto.getAggsList());
    }

    return Lists.newArrayList();
  }

  private void updateDatasetQuery() {
    if (datasetIdProvider == null) return;
    List<String> datasetIds = datasetIdProvider.getIds();
    if (datasetIds.size() > 0) {
      Query datasetQuery = searcher.makeQuery(String.format("in(%s,(%s))", "id", Joiner.on(",").join(datasetIds)));
      setQuery(isQueryNotEmpty() ? searcher.andQuery(getQuery(), datasetQuery) : datasetQuery);
    }
  }

  private Consumer<Dataset> getDatasetConsumer(QueryScope scope, DatasetResultDto.Builder resBuilder,
                                               DatasetCountStatsBuilder datasetCountStatsBuilder) {

    return scope == QueryScope.DETAIL ? (dataset) -> {
      Mica.DatasetDto.Builder datasetBuilder = dtos.asDtoBuilder(dataset);
      if (datasetCountStatsBuilder != null) {
        datasetBuilder.setExtension(MicaSearch.CountStatsDto.datasetCountStats, datasetCountStatsBuilder.build(dataset))
            .build();
      }
      resBuilder.addDatasets(datasetBuilder.build());
    } : (dataset) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(dataset).build());
  }

  @Override
  protected List<String> getAggJoinFields() {
    return Arrays.asList(STUDY_JOIN_FIELD, HARMONIZATION_STUDY_JOIN_FIELD, ID);
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(STUDY_JOIN_FIELD, HARMONIZATION_STUDY_JOIN_FIELD);
  }

  public Map<String, Map<String, List<String>>> getStudyCountsByDataset() {
    Properties props = new Properties();
    props.setProperty("id", "");
    Properties subProps = new Properties();
    getJoinFields().forEach(joinField -> subProps.setProperty(joinField, ""));
    Map<String, Properties> subAggregations = Maps.newHashMap();
    subAggregations.put("id", subProps);


    Map<String, Map<String, List<String>>> map = Maps.newHashMap();
    try {
      Searcher.DocumentResults results = searcher.cover(getSearchIndex(), getSearchType(), getQuery(), props, subAggregations, getAccessibleIdFilter());
      Searcher.DocumentAggregation idAgg = results.getAggregations().stream().filter(agg -> agg.getName().equals("id")).findFirst().get();
      idAgg.asTerms().getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
          .forEach(bucket -> map.put(bucket.getKeyAsString(), getStudyCounts(bucket.getAggregations())));
    } catch (Exception e) {
      log.warn("Study counts by dataset failed", e);
    }

    return map;
  }

  private Map<String, List<String>> getStudyCounts(List<Searcher.DocumentAggregation> aggregations) {
    Map<String, List<String>> map = Maps.newHashMap();
    aggregations.stream().filter(agg -> AggregationHelper.AGG_TERMS.equals(agg.getType()))
        .forEach(aggregation -> map.put(AggregationHelper.unformatName(aggregation.getName()),
            aggregation.asTerms().getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
                .map(Searcher.DocumentTermsBucket::getKeyAsString).collect(Collectors.toList())));

    return map;
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(STUDY_JOIN_FIELD);
  }

  public Map<String, Integer> getHarmonizationStudyCounts() {
    return getDocumentCounts(HARMONIZATION_STUDY_JOIN_FIELD);
  }

}
