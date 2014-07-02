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

import org.obiba.magma.Variable;
import org.obiba.mica.domain.StudyDataset;
import org.obiba.mica.repository.StudyDatasetRepository;
import org.obiba.mica.service.DatasetService;
import org.obiba.mica.service.NoSuchDatasetException;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;

@Service
@Validated
public class StudyDatasetService extends DatasetService {

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  public void save(@NotNull StudyDataset dataset) {
    studyDatasetRepository.save(dataset);
  }

  @Override
  public Iterable<Variable> getVariables(@NotNull String id) throws NoSuchDatasetException {
    StudyDataset dataset = findById(id);
    return null;
  }

  @Override
  public RestValueTable getTable(@NotNull String id) {
    return null;
  }

  /**
   * Get the {@link org.obiba.mica.domain.StudyDataset} fron its id
   *
   * @param id
   * @return
   * @throws org.obiba.mica.service.NoSuchDatasetException
   */
  @NotNull
  public StudyDataset findById(@NotNull String id) throws NoSuchDatasetException {
    StudyDataset dataset = studyDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link org.obiba.mica.domain.StudyDataset}s.
   *
   * @return
   */
  public List<StudyDataset> findAllStudyDatasets() {
    return studyDatasetRepository.findAll();
  }

  /**
   * Get all {@link org.obiba.mica.domain.StudyDataset}s of a study.
   * @param studyId
   * @return
   */
  public List<StudyDataset> findAllStudyDatasets(String studyId) {
    if (Strings.isNullOrEmpty(studyId)) return findAllStudyDatasets();
    return studyDatasetRepository.findByStudyTableStudyId(studyId);
  }

  /**
   * On study deletion, go through all datasets related to this study and remove the dependency.
   *
   * @param event
   */
  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    String studyId = event.getPersistable().getId();

    // TODO
    //findAllStudyDatasets(studyId);
  }
}
