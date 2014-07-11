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

import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexDatasetsEvent;
import org.obiba.mica.service.HarmonizedDatasetService;
import org.obiba.mica.study.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

@Component
public class HarmonizedDatasetIndexer extends DatasetIndexer<HarmonizedDataset> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizedDatasetIndexer.class);

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    if(event.isStudyDataset()) return;
    log.info("Dataset {} was updated", event.getPersistable());
    HarmonizedDataset harmonizedDataset = (HarmonizedDataset) event.getPersistable();
    reIndex(harmonizedDataset);
    harmonizedDataset.getStudyTables().forEach(studyTable -> reIndex(harmonizedDataset, studyTable.getStudyId()));
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    if(event.isStudyDataset()) return;
    log.info("Dataset {} was deleted", event.getPersistable());
    HarmonizedDataset harmonizedDataset = (HarmonizedDataset) event.getPersistable();
    deleteFromDatasetIndices(harmonizedDataset);
    harmonizedDataset.getStudyTables().forEach(studyTable -> deleteFromStudyIndices(harmonizedDataset, studyTable.getStudyId()));
  }

  @Async
  @Subscribe
  public void indexAll(IndexDatasetsEvent event) {
    reIndexAll();
  }

  @Override
  protected Iterable<DatasetVariable> getVariables(HarmonizedDataset dataset) {
    return harmonizedDatasetService.getDatasetVariables(dataset);
  }

  @Override
  protected Iterable<DatasetVariable> getVariables(HarmonizedDataset dataset, Study study) {
    return harmonizedDatasetService.getDatasetVariables(dataset, study);
  }

  @Override
  protected Iterable<HarmonizedDataset> findAllDatasets() {
    List<HarmonizedDataset> datasets = Lists.newArrayList();
    Iterables.addAll(datasets, harmonizedDatasetService.findAllDatasets());
    return datasets;
  }

  protected Iterable<HarmonizedDataset> findAllPublishedDatasets() {
    List<HarmonizedDataset> datasets = Lists.newArrayList();
    Iterables.addAll(datasets, harmonizedDatasetService.findAllPublishedDatasets());
    return datasets;
  }
}
