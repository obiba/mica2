/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset;

import java.util.List;

import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Spring Data MongoDB repository for the {@link HarmonizationDataset} entity.
 */
public interface HarmonizationDatasetRepository extends MongoRepository<HarmonizationDataset, String> {

  List<HarmonizationDataset> findByHarmonizationTableStudyId(String studyId);

  List<HarmonizationDataset> findByStudyTablesStudyId(String studyId);

  List<HarmonizationDataset> findByStudyTablesStudyIdAndStudyTablesPopulationIdAndStudyTablesDataCollectionEventId(
    String studyId, String populationId, String dataCollectionEventId);

  List<HarmonizationDataset> findByHarmonizationTableStudyIdAndHarmonizationTablePopulationId(
    String studyId, String populationId);

  @Query("{'model' : { $exists : false }}")
  List<HarmonizationDataset> findWithoutModel();
}
