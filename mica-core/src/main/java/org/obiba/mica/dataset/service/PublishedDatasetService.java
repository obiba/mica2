/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import java.util.List;

import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;

public interface PublishedDatasetService extends PublishedDocumentService<Dataset>, EsDatasetService {
  long getStudyDatasetsCount();
  long getHarmonizationDatasetsCount();
  List<HarmonizationDataset> getHarmonizationDatasetsByStudy(String studyId);
}
