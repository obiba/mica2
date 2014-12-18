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

import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HarmonizationDatasetIndexer extends AbstractDatasetIndexer<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationDatasetIndexer.class);

  @Override
  public void onDatasetUpdated(HarmonizationDataset dataset) {
    log.info("Dataset {} was updated", dataset);
    reIndexDraft(dataset);
    dataset
        .getStudyTables().forEach(studyTable -> reIndexDraft(dataset, studyTable.getStudyId()));
  }

  @Override
  public void onDatasetPublished(HarmonizationDataset dataset) {
    log.info("Dataset {} was published", dataset);
    reIndexPublished(dataset);
    dataset
        .getStudyTables().forEach(studyTable -> reIndexPublished(dataset, studyTable.getStudyId()));
  }

  @Override
  public void onDatasetDeleted(HarmonizationDataset dataset) {
    log.info("Dataset {} was deleted", dataset);
    deleteFromDatasetIndices(dataset);
  }

  @Override
  public void indexAll(Iterable<HarmonizationDataset> datasets, Iterable<HarmonizationDataset> publishedDatasets) {
    reIndexAll(datasets, publishedDatasets);
  }
}
