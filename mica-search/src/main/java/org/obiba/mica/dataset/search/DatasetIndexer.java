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

import jakarta.inject.Inject;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class DatasetIndexer {

  private static final Logger log = LoggerFactory.getLogger(DatasetIndexer.class);

  private final Lock lock = new ReentrantLock();

  @Inject
  private Indexer indexer;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was updated", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      indexer.index(Indexer.DRAFT_DATASET_INDEX, (Indexable) event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was deleted", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      indexer.delete(Indexer.DRAFT_DATASET_INDEX, (Indexable) event.getPersistable());
      indexer.delete(Indexer.PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      indexer.index(Indexer.PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      indexer.delete(Indexer.PUBLISHED_DATASET_INDEX, (Indexable) event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  synchronized public void reIndexAll(IndexDatasetsEvent event) {
    lock.lock();
    try {
      if (indexer.hasIndex(Indexer.PUBLISHED_DATASET_INDEX)) indexer.dropIndex(Indexer.PUBLISHED_DATASET_INDEX);
      if (indexer.hasIndex(Indexer.DRAFT_DATASET_INDEX)) indexer.dropIndex(Indexer.DRAFT_DATASET_INDEX);

      if (indexer.hasIndex(Indexer.DRAFT_VARIABLE_INDEX)) indexer.dropIndex(Indexer.DRAFT_VARIABLE_INDEX);
      if (indexer.hasIndex(Indexer.PUBLISHED_VARIABLE_INDEX))
        indexer.dropIndex(Indexer.PUBLISHED_VARIABLE_INDEX);
      if (indexer.hasIndex(Indexer.PUBLISHED_HVARIABLE_INDEX))
        indexer.dropIndex(Indexer.PUBLISHED_HVARIABLE_INDEX);

      harmonizedDatasetService.indexAll();
      collectedDatasetService.indexAll();
    } finally {
      lock.unlock();
    }
  }
}
