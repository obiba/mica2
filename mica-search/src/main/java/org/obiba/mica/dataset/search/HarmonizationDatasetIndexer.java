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

import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.dataset.event.IndexHarmonizationDatasetsEvent;
import org.obiba.mica.core.service.HarmonizationDatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

@Component
public class HarmonizationDatasetIndexer extends DatasetIndexer<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationDatasetIndexer.class);

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    if(event.isStudyDataset()) return;
    log.info("Dataset {} was updated", event.getPersistable());
    HarmonizationDataset harmonizationDataset = (HarmonizationDataset) event.getPersistable();
    reIndexDraft(harmonizationDataset);
    harmonizationDataset
        .getStudyTables().forEach(studyTable -> reIndexDraft(harmonizationDataset, studyTable.getStudyId()));
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    if(event.isStudyDataset()) return;
    log.info("Dataset {} was published: {}", event.getPersistable(), event.isPublished());
    HarmonizationDataset harmonizationDataset = (HarmonizationDataset) event.getPersistable();
    reIndexPublished(harmonizationDataset);
    harmonizationDataset
        .getStudyTables().forEach(studyTable -> reIndexPublished(harmonizationDataset, studyTable.getStudyId()));
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    if(event.isStudyDataset()) return;
    log.info("Dataset {} was deleted", event.getPersistable());
    HarmonizationDataset harmonizationDataset = (HarmonizationDataset) event.getPersistable();
    deleteFromDatasetIndices(harmonizationDataset);
  }

  @Async
  @Subscribe
  public void indexAll(IndexDatasetsEvent event) {
    reIndexAll();
  }

  @Async
  @Subscribe
  public void indexAll(IndexHarmonizationDatasetsEvent event) {
    reIndexAll();
  }

  @Override
  protected Iterable<HarmonizationDataset> findAllDatasets() {
    List<HarmonizationDataset> datasets = Lists.newArrayList();
    Iterables.addAll(datasets, harmonizationDatasetService.findAllDatasets());
    return datasets;
  }

  protected Iterable<HarmonizationDataset> findAllPublishedDatasets() {
    List<HarmonizationDataset> datasets = Lists.newArrayList();
    Iterables.addAll(datasets, harmonizationDatasetService.findAllPublishedDatasets());
    return datasets;
  }
}
