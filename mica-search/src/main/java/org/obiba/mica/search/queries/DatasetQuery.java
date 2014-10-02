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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static org.obiba.mica.search.CountStatsDtoBuilders.DatasetCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.DatasetResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class DatasetQuery extends AbstractDocumentQuery {


  private static final String DATASET_FACETS_YML = "dataset-facets.yml";

  private static final String STUDY_JOIN_FIELD = "studyTable.studyId";

  private static final String HARMONIZATION_JOIN_FIELD = "studyTables.studyId";

  @Inject
  Dtos dtos;

  @Inject
  PublishedDatasetService publishedDatasetService;

  @Override
  public String getSearchIndex() {
    return DatasetIndexer.PUBLISHED_DATASET_INDEX;
  }

  @Override
  public String getSearchType() {
    return DatasetIndexer.DATASET_TYPE;
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(DATASET_FACETS_YML);
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope,
      CountStatsData counts) {
    DatasetResultDto.Builder resBuilder = DatasetResultDto.newBuilder();
    DatasetCountStatsBuilder datasetCountStatsBuilder = counts == null
        ? null
        : DatasetCountStatsBuilder.newBuilder(counts);

    Consumer<Dataset> addDto = getDatasetConsumer(scope, resBuilder, datasetCountStatsBuilder);

    for (SearchHit hit : hits) {
      addDto.accept(publishedDatasetService.findById(hit.getId()));
    }

    builder.setExtension(DatasetResultDto.result, resBuilder.build());
  }

  private Consumer<Dataset> getDatasetConsumer(Scope scope, DatasetResultDto.Builder resBuilder,
      DatasetCountStatsBuilder datasetCountStatsBuilder) {

    return scope == Scope.DETAIL
      ? (dataset) -> {
        Mica.DatasetDto.Builder datasetBuilder = dtos.asDtoBuilder(dataset);
        if(datasetCountStatsBuilder != null) {
          datasetBuilder
              .setExtension(MicaSearch.CountStatsDto.datasetCountStats, datasetCountStatsBuilder.build(dataset))
              .build();
        }
        resBuilder.addDatasets(datasetBuilder.build());
      }
      : (dataset) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(dataset).build());
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(STUDY_JOIN_FIELD, HARMONIZATION_JOIN_FIELD);
  }

  public Map<String, Integer> getStudyCounts() {
    return getStudyCounts(STUDY_JOIN_FIELD);
  }

  public Map<String, Integer> getHarmonizationStudyCounts() {
    return getStudyCounts(HARMONIZATION_JOIN_FIELD);
  }

}
