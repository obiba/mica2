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
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
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

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void indexAll(IndexDatasetsEvent event) {
    reIndexAll(DRAFT_DATASET_INDEX, findAllDatasets());
    reIndexAll(PUBLISHED_DATASET_INDEX, findAllPublishedDatasets());
  }

  private void reIndexAll(String indexName, Iterable<Dataset> datasets) {
    if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    elasticSearchIndexer.indexAll(indexName, datasets);
    for(Dataset dataset : datasets) {
      if(dataset instanceof StudyDataset) {
        elasticSearchIndexer.indexAllIndexables(indexName, Iterables
            .transform(studyDatasetService.getVariables(dataset.getId()),
                input -> new DatasetVariable((StudyDataset) dataset, input)));
      } else {
        elasticSearchIndexer.indexAllIndexables(indexName, Iterables
            .transform(harmonizedDatasetService.getVariables(dataset.getId()),
                input -> new DatasetVariable((HarmonizedDataset) dataset, input)));
      }
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
