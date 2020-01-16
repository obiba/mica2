/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import com.google.common.eventbus.Subscribe;
import org.obiba.mica.dataset.event.*;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DatasetIndexer {

  private static final Logger log = LoggerFactory.getLogger(DatasetIndexer.class);

  @Inject
  private Indexer indexer;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    log.debug("{} {} was updated", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.index(Indexer.DRAFT_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    log.debug("{} {} was deleted", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.delete(Indexer.DRAFT_DATASET_INDEX, (Indexable) event.getPersistable());
    indexer.delete(Indexer.PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.index(Indexer.PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    indexer.delete(Indexer.PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  synchronized public void reIndexAll(IndexDatasetsEvent event) {
    if (indexer.hasIndex(Indexer.PUBLISHED_DATASET_INDEX)) indexer.dropIndex(Indexer.PUBLISHED_DATASET_INDEX);
    if (indexer.hasIndex(Indexer.DRAFT_DATASET_INDEX)) indexer.dropIndex(Indexer.DRAFT_DATASET_INDEX);

    if (indexer.hasIndex(Indexer.DRAFT_VARIABLE_INDEX)) indexer.dropIndex(Indexer.DRAFT_VARIABLE_INDEX);
    if (indexer.hasIndex(Indexer.PUBLISHED_VARIABLE_INDEX))
      indexer.dropIndex(Indexer.PUBLISHED_VARIABLE_INDEX);
    if (indexer.hasIndex(Indexer.PUBLISHED_HVARIABLE_INDEX))
      indexer.dropIndex(Indexer.PUBLISHED_HVARIABLE_INDEX);

    harmonizedDatasetService.indexAll();
    collectedDatasetService.indexAll();
  }
}
