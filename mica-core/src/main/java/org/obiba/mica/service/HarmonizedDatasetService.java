/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.service;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.HarmonizedDataset;
import org.obiba.mica.repository.HarmonizedDatasetRepository;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;

@Service
@Validated
public class HarmonizedDatasetService extends DatasetService {

  @Inject
  private HarmonizedDatasetRepository harmonizedDatasetRepository;

  public void save(@NotNull HarmonizedDataset dataset) {
    harmonizedDatasetRepository.save(dataset);
  }

  /**
   * Get the {@link org.obiba.mica.domain.HarmonizedDataset} from its id.
   *
   * @param id
   * @return
   * @throws NoSuchDatasetException
   */
  @NotNull
  public HarmonizedDataset findHarmonizedDatasetById(@NotNull String id) throws NoSuchDatasetException {
    HarmonizedDataset dataset = harmonizedDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link org.obiba.mica.domain.HarmonizedDataset}s.
   *
   * @return
   */
  public List<HarmonizedDataset> findAllHarmonizedDatasets() {
    return harmonizedDatasetRepository.findAll();
  }

  /**
   * Get all {@link org.obiba.mica.domain.HarmonizedDataset}s having a reference to the given study.
   * @param studyId
   * @return
   */
  public List<HarmonizedDataset> findAllHarmonizedDatasets(String studyId) {
    if (Strings.isNullOrEmpty(studyId)) return findAllHarmonizedDatasets();
    return harmonizedDatasetRepository.findByStudyTablesStudyId(studyId);
  }

  /**
   * On study deletion, go through all datasets related to this study and remove the dependency.
   *
   * @param event
   */
  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {

  }
}
