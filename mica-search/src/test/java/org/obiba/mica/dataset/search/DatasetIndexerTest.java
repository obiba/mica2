/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import org.junit.jupiter.api.Test;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DatasetIndexerTest {

  /**
   * Indexing publishes dataset events, and those event handlers need the same lock reIndexAll uses,
   * on the same bounded pool it runs on. When the lock spanned the whole reindex, every handler
   * dispatched during it parked forever, exhausted the pool, and the reindex could never finish.
   */
  @Test
  public void perDatasetEventIsNotBlockedByRunningReIndexAll() throws Exception {
    Indexer indexer = mock(Indexer.class);
    HarmonizedDatasetService harmonizedDatasetService = mock(HarmonizedDatasetService.class);
    CollectedDatasetService collectedDatasetService = mock(CollectedDatasetService.class);

    CountDownLatch reIndexAllReachedIndexing = new CountDownLatch(1);
    CountDownLatch releaseIndexing = new CountDownLatch(1);

    doAnswer(invocation -> {
      reIndexAllReachedIndexing.countDown();
      assertThat(releaseIndexing.await(10, TimeUnit.SECONDS)).isTrue();
      return null;
    }).when(harmonizedDatasetService).indexAll();

    DatasetIndexer datasetIndexer =
      new DatasetIndexer(indexer, harmonizedDatasetService, collectedDatasetService);

    ExecutorService threads = Executors.newFixedThreadPool(2);
    try {
      threads.submit(() -> datasetIndexer.reIndexAll(new IndexDatasetsEvent()));
      assertThat(reIndexAllReachedIndexing.await(5, TimeUnit.SECONDS))
        .as("reIndexAll should have reached the indexing phase")
        .isTrue();

      StudyDataset dataset = new StudyDataset();
      dataset.setId("collected-dataset-1");
      Future<?> perDatasetEvent =
        threads.submit(() -> datasetIndexer.datasetUpdated(new DatasetUpdatedEvent(dataset)));

      // times out while the lock spans the whole reindex
      perDatasetEvent.get(5, TimeUnit.SECONDS);
      verify(indexer).index(eq(Indexer.DRAFT_DATASET_INDEX), any(Indexable.class));
    } finally {
      releaseIndexing.countDown();
      threads.shutdownNow();
    }
  }

  @Test
  public void reIndexAllDropsIndexesBeforeIndexing() {
    Indexer indexer = mock(Indexer.class);
    HarmonizedDatasetService harmonizedDatasetService = mock(HarmonizedDatasetService.class);
    CollectedDatasetService collectedDatasetService = mock(CollectedDatasetService.class);

    new DatasetIndexer(indexer, harmonizedDatasetService, collectedDatasetService)
      .reIndexAll(new IndexDatasetsEvent());

    verify(harmonizedDatasetService).indexAll();
    verify(collectedDatasetService).indexAll();
  }
}
