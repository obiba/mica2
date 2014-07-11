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

import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.Variable;
import org.obiba.mica.dataset.DatasourceRegistry;
import org.obiba.mica.dataset.HarmonizedDatasetRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizedDataset;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.service.DatasetService;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.StudyService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Service
@Validated
public class HarmonizedDatasetService extends DatasetService<HarmonizedDataset> {

  @Inject
  private StudyService studyService;

  @Inject
  private DatasourceRegistry datasourceRegistry;

  @Inject
  private HarmonizedDatasetRepository harmonizedDatasetRepository;

  @Inject
  private EventBus eventBus;

  public void save(@NotNull HarmonizedDataset dataset) {
    harmonizedDatasetRepository.save(dataset);
    eventBus.post(new DatasetUpdatedEvent(dataset));
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.HarmonizedDataset} from its id.
   *
   * @param id
   * @return
   * @throws org.obiba.mica.dataset.NoSuchDatasetException
   */
  @NotNull
  public HarmonizedDataset findById(@NotNull String id) throws NoSuchDatasetException {
    HarmonizedDataset dataset = harmonizedDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.HarmonizedDataset}s.
   *
   * @return
   */
  public List<HarmonizedDataset> findAllDatasets() {
    return harmonizedDatasetRepository.findAll();
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.HarmonizedDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizedDataset> findAllDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllDatasets();
    return harmonizedDatasetRepository.findByStudyTablesStudyId(studyId);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.HarmonizedDataset}s.
   *
   * @return
   */
  public List<HarmonizedDataset> findAllPublishedDatasets() {
    return harmonizedDatasetRepository.findByPublished(true);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.HarmonizedDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizedDataset> findAllPublishedDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllPublishedDatasets();
    return harmonizedDatasetRepository.findByStudyTablesStudyIdAndPublished(studyId, true);
  }

  /**
   * Index the dataset and associated variables.
   *
   * @param id
   */
  public void index(@NotNull String id) {
    HarmonizedDataset dataset = findById(id);
    eventBus.post(new DatasetUpdatedEvent(dataset));
  }

  /**
   * Apply dataset publication flag.
   *
   * @param id
   * @param published
   */
  public void publish(@NotNull String id, boolean published) {
    HarmonizedDataset dataset = findById(id);
    dataset.setPublished(published);
    save(dataset);
  }

  /**
   * Check if a dataset is published.
   *
   * @param id
   * @return
   */
  public boolean isPublished(@NotNull String id) throws NoSuchDatasetException {
    HarmonizedDataset dataset = findById(id);
    return dataset.isPublished();
  }

  @Override
  @NotNull
  protected RestValueTable getTable(@NotNull HarmonizedDataset dataset)
      throws NoSuchDatasetException, NoSuchValueTableException {
    return execute(dataset.getProject(), datasource -> (RestValueTable) datasource.getValueTable(dataset.getTable()));
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(HarmonizedDataset dataset) {
    return Iterables.transform(getVariables(dataset), input -> new DatasetVariable(dataset, input));
  }

  @Override
  public DatasetVariable getDatasetVariable(HarmonizedDataset dataset, String variableName) {
    return new DatasetVariable(dataset, getVariableValueSource(dataset, variableName).getVariable());
  }

  public Iterable<DatasetVariable> getDatasetVariables(HarmonizedDataset dataset, String studyId) {
    return Iterables
        .transform(getVariables(dataset, studyId), input -> new DatasetVariable(dataset, input, studyId));
  }

  public DatasetVariable getDatasetVariable(HarmonizedDataset dataset, String variableName, String studyId) {
    return new DatasetVariable(dataset, getTable(dataset, studyId).getVariableValueSource(variableName).getVariable());
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

  @Override
  protected DatasourceRegistry getDatasourceRegistry() {
    return datasourceRegistry;
  }

  @Override
  protected StudyService getStudyService() {
    return studyService;
  }

  @Override
  protected EventBus getEventBus() {
    return eventBus;
  }

  //
  // Private methods
  //

  private Iterable<Variable> getVariables(@NotNull HarmonizedDataset dataset, String studyId)
      throws NoSuchDatasetException {
    return getTable(dataset, studyId).getVariables();
  }

  private RestValueTable getTable(@NotNull HarmonizedDataset dataset, String studyId) {
    for (StudyTable studyTable : dataset.getStudyTables()) {
      if (studyTable.getStudyId().equals(studyId)) {
        return execute(studyTable.getProject(), ds -> (RestValueTable)ds.getValueTable(studyTable.getTable()));
      }
    }
    throw NoSuchStudyException.withId(studyId);
  }

  /**
   * Build or reuse the {@link org.obiba.opal.rest.client.magma.RestDatasource} and execute the callback with it.
   *
   * @param project
   * @param callback
   * @param <T>
   * @return
   */
  private <T> T execute(String project, DatasourceCallback<T> callback) {
    return execute(getDatasource(project), callback);
  }

}
