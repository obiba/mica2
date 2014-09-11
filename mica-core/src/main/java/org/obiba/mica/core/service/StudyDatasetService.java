/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.DatasourceConnectionPool;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexStudyDatasetsEvent;
import org.obiba.mica.dataset.service.DatasetService;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.service.StudyService;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.obiba.opal.web.model.Search;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Service
@Validated
public class StudyDatasetService extends DatasetService<StudyDataset> {

  @Inject
  private StudyService studyService;

  @Inject
  private DatasourceConnectionPool datasourceConnectionPool;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private EventBus eventBus;

  public void save(@NotNull StudyDataset dataset) {
    StudyDataset saved = dataset;
    if(saved.isNew()) {
      generateId(dataset);
      saved.setId(getNextId(saved.getAcronym()));
    } else {
      saved = findById(dataset.getId());
      BeanUtils.copyProperties(dataset, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
    }
    studyDatasetRepository.save(saved);
    eventBus.post(new DatasetUpdatedEvent(saved));
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.StudyDataset} fron its id
   *
   * @param id
   * @return
   * @throws org.obiba.mica.dataset.NoSuchDatasetException
   */
  @NotNull
  public StudyDataset findById(@NotNull String id) throws NoSuchDatasetException {
    StudyDataset dataset = studyDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.StudyDataset}s.
   *
   * @return
   */
  public List<StudyDataset> findAllDatasets() {
    return studyDatasetRepository.findAll();
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.StudyDataset}s of a study.
   *
   * @param studyId
   * @return
   */
  public List<StudyDataset> findAllDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllDatasets();
    return studyDatasetRepository.findByStudyTableStudyId(studyId);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.StudyDataset}s.
   *
   * @return
   */
  public List<StudyDataset> findAllPublishedDatasets() {
    return studyDatasetRepository.findByPublished(true);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.StudyDataset}s of a study.
   *
   * @param studyId
   * @return
   */
  public List<StudyDataset> findAllPublishedDatasets(@Nullable String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllPublishedDatasets();
    return studyDatasetRepository.findByStudyTableStudyIdAndPublished(studyId, true);
  }

  /**
   * Apply dataset publication flag.
   *
   * @param id
   * @param published
   */
  public void publish(@NotNull String id, boolean published) {
    StudyDataset dataset = findById(id);
    dataset.setPublished(published);
    save(dataset);
    eventBus.post(new DatasetPublishedEvent(dataset));
  }

  /**
   * Check if a dataset is published.
   *
   * @param id
   * @return
   */
  public boolean isPublished(@NotNull String id) throws NoSuchDatasetException {
    StudyDataset dataset = findById(id);
    return dataset.isPublished();
  }

  /**
   * Index the dataset and associated variables.
   *
   * @param id
   */
  public void index(@NotNull String id) {
    StudyDataset dataset = findById(id);
    eventBus.post(new DatasetUpdatedEvent(dataset));
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll() {
    getEventBus().post(new IndexStudyDatasetsEvent());
  }

  @Override
  @NotNull
  protected RestValueTable getTable(@NotNull StudyDataset dataset) throws NoSuchValueTableException {
    StudyTable studyTable = dataset.getStudyTable();
    return execute(studyTable, datasource -> (RestValueTable) datasource.getValueTable(studyTable.getTable()));
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(StudyDataset dataset) {
    return Iterables.transform(getVariables(dataset), input -> new DatasetVariable(dataset, input));
  }

  @Override
  public DatasetVariable getDatasetVariable(StudyDataset dataset, String variableName)
      throws NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getVariableValueSource(dataset, variableName).getVariable());
  }

  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary(@NotNull StudyDataset dataset,
      String variableName) throws NoSuchValueTableException, NoSuchVariableException {
    return getVariableValueSource(dataset, variableName).getSummary();
  }

  public Search.QueryResultDto getVariableFacet(@NotNull StudyDataset dataset, String variableName)
      throws NoSuchValueTableException, NoSuchVariableException {
    return getVariableValueSource(dataset, variableName).getFacet();
  }

  public Search.QueryResultDto getFacets(@NotNull StudyDataset dataset, Search.QueryTermsDto query)
      throws NoSuchValueTableException, NoSuchVariableException {
    return getTable(dataset).getFacets(query);
  }

  /**
   * On study deletion, go through all datasets related to this study, then remove the dependency
   * and clean the variable index.
   *
   * @param event
   */
  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    String studyId = event.getPersistable().getId();

    // TODO
    //findAllDatasets(studyId);
  }

  @Override
  protected DatasourceConnectionPool getDatasourceConnectionPool() {
    return datasourceConnectionPool;
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

  /**
   * Build or reuse the {@link org.obiba.opal.rest.client.magma.RestDatasource} and execute the callback with it.
   *
   * @param studyTable
   * @param callback
   * @param <T>
   * @return
   */
  private <T> T execute(StudyTable studyTable, DatasourceCallback<T> callback) {
    return execute(getDatasource(studyTable), callback);
  }

}
