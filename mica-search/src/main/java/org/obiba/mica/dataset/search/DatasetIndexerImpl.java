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

import javax.inject.Inject;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.core.domain.Indexable;
import org.obiba.mica.dataset.service.DatasetIndexer;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetIndexerImpl implements DatasetIndexer {

  private static final Logger log = LoggerFactory.getLogger(DatasetIndexerImpl.class);

  public static final String DRAFT_DATASET_INDEX = "dataset-draft";

  public static final String PUBLISHED_DATASET_INDEX = "dataset-published";

  public static final String DATASET_TYPE = Dataset.MAPPING_NAME;

  public static final String[] LOCALIZED_ANALYZED_FIELDS = {"acronym", "name", "description"};

  @Inject
  protected ElasticSearchIndexer elasticSearchIndexer;

  @Override
  public void onDatasetUpdated(Dataset dataset) {
    log.info("Dataset {} was updated", dataset);
    reIndexDraft(dataset);
  }

  @Override
  public void onDatasetPublished(Dataset dataset) {
    log.info("Dataset {} was published", dataset);
    reIndexPublished(dataset);
  }

  @Override
  public void onDatasetDeleted(Dataset dataset) {
    log.info("Dataset {} was deleted", dataset);
    deleteFromDatasetIndices(dataset);
  }

  @Override
  public void indexAll(Iterable<? extends Dataset> datasets,
    Iterable<? extends Dataset> publishedDatasets) {
    reIndexAll(datasets, publishedDatasets);
  }

  @Override
  public void dropIndex() {
    if (elasticSearchIndexer.hasIndex(DRAFT_DATASET_INDEX)) elasticSearchIndexer.dropIndex(DRAFT_DATASET_INDEX);
    if (elasticSearchIndexer.hasIndex(PUBLISHED_DATASET_INDEX)) elasticSearchIndexer.dropIndex(PUBLISHED_DATASET_INDEX);
  }

  protected void reIndexDraft(Dataset dataset) {
    deleteFromDatasetIndex(DRAFT_DATASET_INDEX, dataset);
    elasticSearchIndexer.index(DRAFT_DATASET_INDEX, (Indexable) dataset);
  }

  protected void reIndexPublished(Dataset dataset) {
    deleteFromDatasetIndex(PUBLISHED_DATASET_INDEX, dataset);
    if(dataset.isPublished()) {
      elasticSearchIndexer.index(PUBLISHED_DATASET_INDEX, (Indexable) dataset);
    }
  }

  protected void deleteFromDatasetIndices(Dataset dataset) {
    deleteFromDatasetIndex(DRAFT_DATASET_INDEX, dataset);
    deleteFromDatasetIndex(PUBLISHED_DATASET_INDEX, dataset);
  }

  private void deleteFromDatasetIndex(String indexName, Dataset dataset) {
    elasticSearchIndexer.delete(indexName, (Indexable) dataset);
  }

  protected void reIndexAll(Iterable<? extends Dataset> datasets, Iterable<? extends Dataset> publishedDatasets) {
    reIndexAll(DRAFT_DATASET_INDEX, datasets);
    reIndexAll(PUBLISHED_DATASET_INDEX, publishedDatasets);
  }

  private void reIndexAll(String indexName, Iterable<? extends Dataset> datasets) {
    //if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    // TODO delete dataset by their className and delete their children as-well
    elasticSearchIndexer.indexAllIndexables(indexName, datasets);
  }
}
