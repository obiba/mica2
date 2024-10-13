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

import org.obiba.mica.core.repository.DocumentRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Spring Data MongoDB repository for the {@link HarmonizationDataset} entity.
 */
public interface HarmonizationDatasetRepository extends MongoRepository<HarmonizationDataset, String>, DocumentRepository<HarmonizationDataset> {

  List<HarmonizationDataset> findByHarmonizationTableStudyId(String studyId);

  List<HarmonizationDataset> findByStudyTablesStudyId(String studyId);

  @Query("{'studyTables' : { $elemMatch: { 'studyId': ?0, 'populationId': ?1, 'dataCollectionEventId': ?2 } }}")
  List<HarmonizationDataset> findByStudyTablesStudyIdAndStudyTablesPopulationIdAndStudyTablesDataCollectionEventId(
    String studyId, String populationId, String dataCollectionEventId);

  @Query("{'model' : { $exists : false }}")
  List<HarmonizationDataset> findWithoutModel();

  @Query(value = "{}", fields = "{_id : 1}")
  List<HarmonizationDataset> findAllExistingIds();
}
