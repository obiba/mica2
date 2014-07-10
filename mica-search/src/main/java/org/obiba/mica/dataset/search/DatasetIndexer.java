/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.domain.Indexable;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.service.StudyDatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

@Component
public class DatasetIndexer {

  private static final Logger log = LoggerFactory.getLogger(DatasetIndexer.class);

  public static final String DRAFT_DATASET_INDEX = "dataset-draft";

  public static final String PUBLISHED_DATASET_INDEX = "dataset-published";

  public static final String VARIABLE_TYPE = DatasetVariable.MAPPING_NAME;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    log.info("Dataset {} was updated", event.getPersistable());
    reIndex(event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    log.info("Dataset {} was deleted", event.getPersistable());
    deleteFromIndex(event.getPersistable());
  }

  @Async
  @Subscribe
  public void indexAll(IndexDatasetsEvent event) {
    reIndexAll(DRAFT_DATASET_INDEX, findAllDatasets());
    reIndexAll(PUBLISHED_DATASET_INDEX, findAllPublishedDatasets());
  }

  private void reIndex(Dataset dataset) {
    deleteFromIndex(dataset);
    elasticSearchIndexer.index(DRAFT_DATASET_INDEX, (Indexable) dataset);
    indexDatasetVariables(DRAFT_DATASET_INDEX, dataset);
    if(dataset.isPublished()) {
      elasticSearchIndexer.index(PUBLISHED_DATASET_INDEX, (Indexable) dataset);
      indexDatasetVariables(PUBLISHED_DATASET_INDEX, dataset);
    }
  }

  private void deleteFromIndex(Dataset dataset) {
    // TODO delete variables from indices
    elasticSearchIndexer.delete(DRAFT_DATASET_INDEX, (Indexable) dataset);
    elasticSearchIndexer.delete(PUBLISHED_DATASET_INDEX, (Indexable) dataset);
  }

  private void reIndexAll(String indexName, Iterable<Dataset> datasets) {
    if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    elasticSearchIndexer.indexAllIndexables(indexName, datasets);
    datasets.forEach(dataset -> indexDatasetVariables(indexName, dataset));
  }

  private void indexDatasetVariables(String indexName, Dataset dataset) {
    try {
      if(dataset instanceof StudyDataset) {
        elasticSearchIndexer
            .indexAllIndexables(indexName, studyDatasetService.getDatasetVariables((StudyDataset) dataset), dataset);
      } else {
        elasticSearchIndexer
            .indexAllIndexables(indexName, harmonizedDatasetService.getDatasetVariables((HarmonizedDataset) dataset),
                dataset);
      }
    } catch(Exception e) {
      log.error("Unable to index variables of dataset: {}", dataset, e);
    }
  }

  private Iterable<Dataset> findAllDatasets() {
    List<Dataset> datasets = Lists.newArrayList();
    Iterables.addAll(datasets, studyDatasetService.findAllDatasets());
    Iterables.addAll(datasets, harmonizedDatasetService.findAllDatasets());
    return datasets;
  }

  private Iterable<Dataset> findAllPublishedDatasets() {
    List<Dataset> datasets = Lists.newArrayList();
    Iterables.addAll(datasets, studyDatasetService.findAllPublishedDatasets());
    Iterables.addAll(datasets, harmonizedDatasetService.findAllPublishedDatasets());
    return datasets;
  }
}
