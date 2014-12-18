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

import org.obiba.mica.dataset.domain.StudyDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class StudyDatasetIndexer extends AbstractDatasetIndexer<StudyDataset> {

  private static final Logger log = LoggerFactory.getLogger(StudyDatasetIndexer.class);

  @Override
  public void onDatasetUpdated(StudyDataset dataset) {
    log.info("Dataset {} was updated", dataset);
    reIndexDraft(dataset);
    reIndexDraft(dataset, dataset.getStudyTable().getStudyId());
  }

  @Override
  public void onDatasetPublished(StudyDataset dataset) {
    log.info("Dataset {} was published", dataset);
    reIndexPublished(dataset);
    reIndexPublished(dataset, dataset.getStudyTable().getStudyId());
  }

  @Override
  public void onDatasetDeleted(StudyDataset dataset) {
    log.info("Dataset {} was deleted", dataset);
    deleteFromDatasetIndices(dataset);
  }

  @Override
  public void indexAll(Iterable<StudyDataset> datasets, Iterable<StudyDataset> publishedDatasets) {
    reIndexAll(datasets, publishedDatasets);
  }
}
