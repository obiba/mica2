/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class DatasetIndexer {

  private static final Logger log = LoggerFactory.getLogger(DatasetIndexer.class);

  public static final String DRAFT_DATASET_INDEX = "dataset-draft";

  public static final String PUBLISHED_DATASET_INDEX = "dataset-published";

  public static final String DATASET_TYPE = Dataset.MAPPING_NAME;

  public static final String[] LOCALIZED_ANALYZED_FIELDS = { "acronym", "name", "description" };

  @Inject
  private Indexer indexer;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private CollectionDatasetService collectionDatasetService;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    log.debug("{} {} was updated", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.index(DRAFT_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    log.debug("{} {} was deleted", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.delete(DRAFT_DATASET_INDEX, (Indexable) event.getPersistable());
    indexer.delete(PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.index(PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.delete(PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  synchronized public void reIndexAll(IndexDatasetsEvent event) {
    if(indexer.hasIndex(PUBLISHED_DATASET_INDEX)) indexer.dropIndex(PUBLISHED_DATASET_INDEX);
    if(indexer.hasIndex(DRAFT_DATASET_INDEX)) indexer.dropIndex(DRAFT_DATASET_INDEX);

    if(indexer.hasIndex(VariableIndexer.DRAFT_VARIABLE_INDEX)) indexer.dropIndex(VariableIndexer.DRAFT_VARIABLE_INDEX);
    if(indexer.hasIndex(VariableIndexer.PUBLISHED_VARIABLE_INDEX)) indexer.dropIndex(VariableIndexer.PUBLISHED_VARIABLE_INDEX);

    harmonizationDatasetService.indexAll();
    collectionDatasetService.indexAll();
  }
}
