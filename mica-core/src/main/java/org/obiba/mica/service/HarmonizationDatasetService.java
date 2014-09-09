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
import org.obiba.magma.NoSuchVariableException;
import org.obiba.magma.Variable;
import org.obiba.mica.dataset.DatasourceConnectionPool;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexHarmonizationDatasetsEvent;
import org.obiba.mica.dataset.service.DatasetService;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.study.event.StudyDeletedEvent;
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
public class HarmonizationDatasetService extends DatasetService<HarmonizationDataset> {

  @Inject
  private StudyService studyService;

  @Inject
  private DatasourceConnectionPool datasourceConnectionPool;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private EventBus eventBus;

  public void save(@NotNull HarmonizationDataset dataset) {
    HarmonizationDataset saved = dataset;
    if(!Strings.isNullOrEmpty(dataset.getId())) {
      saved = findById(dataset.getId());
      BeanUtils.copyProperties(dataset, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
    }
    harmonizationDatasetRepository.save(saved);
    eventBus.post(new DatasetUpdatedEvent(saved));
  }

  /**
   * Get the {@link org.obiba.mica.dataset.domain.HarmonizationDataset} from its id.
   *
   * @param id
   * @return
   * @throws org.obiba.mica.dataset.NoSuchDatasetException
   */
  @NotNull
  public HarmonizationDataset findById(@NotNull String id) throws NoSuchDatasetException {
    HarmonizationDataset dataset = harmonizationDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s.
   *
   * @return
   */
  public List<HarmonizationDataset> findAllDatasets() {
    return harmonizationDatasetRepository.findAll();
  }

  /**
   * Get all {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizationDataset> findAllDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllDatasets();
    return harmonizationDatasetRepository.findByStudyTablesStudyId(studyId);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s.
   *
   * @return
   */
  public List<HarmonizationDataset> findAllPublishedDatasets() {
    return harmonizationDatasetRepository.findByPublished(true);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizationDataset> findAllPublishedDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllPublishedDatasets();
    return harmonizationDatasetRepository.findByStudyTablesStudyIdAndPublished(studyId, true);
  }

  /**
   * Index the dataset and associated variables.
   *
   * @param id
   */
  public void index(@NotNull String id) {
    HarmonizationDataset dataset = findById(id);
    eventBus.post(new DatasetUpdatedEvent(dataset));
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll() {
    getEventBus().post(new IndexHarmonizationDatasetsEvent());
  }

  /**
   * Apply dataset publication flag.
   *
   * @param id
   * @param published
   */
  public void publish(@NotNull String id, boolean published) {
    HarmonizationDataset dataset = findById(id);
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
    HarmonizationDataset dataset = findById(id);
    return dataset.isPublished();
  }

  @Override
  @NotNull
  protected RestValueTable getTable(@NotNull HarmonizationDataset dataset) throws NoSuchValueTableException {
    return execute(dataset.getProject(), datasource -> (RestValueTable) datasource.getValueTable(dataset.getTable()));
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(HarmonizationDataset dataset) throws NoSuchValueTableException {
    return Iterables.transform(getVariables(dataset), input -> new DatasetVariable(dataset, input));
  }

  @Override
  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName)
      throws NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getVariableValueSource(dataset, variableName).getVariable());
  }

  public Iterable<DatasetVariable> getDatasetVariables(HarmonizationDataset dataset, String studyId)
      throws NoSuchStudyException, NoSuchValueTableException {
    return Iterables.transform(getVariables(dataset, studyId), input -> new DatasetVariable(dataset, input, studyId));
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, String studyId)
      throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getTable(dataset, studyId).getVariableValueSource(variableName).getVariable());
  }

  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary(@NotNull HarmonizationDataset dataset,
      String variableName, String studyId)
      throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return getVariableValueSource(dataset, variableName, studyId).getSummary();
  }

  public Search.QueryResultDto getVariableFacet(@NotNull HarmonizationDataset dataset, String variableName,
      String studyId) throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return getVariableValueSource(dataset, variableName, studyId).getFacet();
  }

  public Search.QueryResultDto getFacets(@NotNull HarmonizationDataset dataset, Search.QueryTermsDto query,
      String studyId) throws NoSuchStudyException, NoSuchValueTableException {
    return getTable(dataset, studyId).getFacets(query);
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

  private Iterable<Variable> getVariables(@NotNull HarmonizationDataset dataset, String studyId)
      throws NoSuchDatasetException, NoSuchStudyException, NoSuchValueTableException {
    return getTable(dataset, studyId).getVariables();
  }

  private RestValueTable getTable(@NotNull HarmonizationDataset dataset, String studyId)
      throws NoSuchStudyException, NoSuchValueTableException {
    for(StudyTable studyTable : dataset.getStudyTables()) {
      if(studyTable.getStudyId().equals(studyId)) {
        return execute(studyTable, ds -> (RestValueTable) ds.getValueTable(studyTable.getTable()));
      }
    }
    throw NoSuchStudyException.withId(studyId);
  }

  private RestValueTable.RestVariableValueSource getVariableValueSource(@NotNull HarmonizationDataset dataset,
      String variableName, String studyId)
      throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    for(StudyTable studyTable : dataset.getStudyTables()) {
      if(studyTable.getStudyId().equals(studyId)) {
        return (RestValueTable.RestVariableValueSource) getTable(dataset, studyId).getVariableValueSource(variableName);
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
