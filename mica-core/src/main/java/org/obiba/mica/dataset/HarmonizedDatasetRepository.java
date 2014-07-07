/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset;

import java.util.List;

import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the {@link org.obiba.mica.dataset.domain.HarmonizedDataset} entity.
 */
public interface HarmonizedDatasetRepository extends MongoRepository<HarmonizedDataset, String> {

  List<HarmonizedDataset> findByStudyTablesStudyId(String studyId);

  List<HarmonizedDataset> findByPublished(boolean published);

  List<HarmonizedDataset> findByStudyTablesStudyIdAndPublished(String studyId, boolean published);

}
