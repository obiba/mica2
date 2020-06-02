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

import org.obiba.mica.dataset.domain.StudyDataset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Spring Data MongoDB repository for the {@link org.obiba.mica.dataset.domain.StudyDataset} entity.
 */
public interface StudyDatasetRepository extends MongoRepository<StudyDataset, String> {

  List<StudyDataset> findByStudyTableStudyId(String studyId);

  List<StudyDataset> findByStudyTableStudyIdAndStudyTablePopulationIdAndStudyTableDataCollectionEventId(
    String studyId, String populationId, String dataCollectionEventId);

  @Query("{'model' : { $exists : false }}")
  List<StudyDataset> findWithoutModel();

  @Query(value = "{}", fields = "{_id : 1}")
  List<StudyDataset> findAllExistingIds();

  List<StudyDataset> findByIdNotIn(Iterable<String> ids);
}
