/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.IndexStudyDatasetsEvent;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.study.service.StudyService;
import org.obiba.opal.rest.client.magma.RestValueTable;
import org.obiba.opal.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

@Service
@Validated
public class StudyDatasetService extends DatasetService<StudyDataset> {
  private static final Logger log = LoggerFactory.getLogger(StudyDatasetService.class);

  @Inject
  private StudyService studyService;

  @Inject
  private OpalService opalService;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private DatasetIndexer<StudyDataset> datasetIndexer;

  @Inject
  private VariableIndexer variableIndexer;

  @Inject
  @Lazy
  private Helper helper;

  public void save(@NotNull StudyDataset dataset) {
    save(dataset, false);
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
  @Caching(evict = {
    @CacheEvict(value = "aggregations-metadata", key = "'dataset'")
  })
  public void publish(@NotNull String id, boolean published) {
    StudyDataset dataset = findById(id);
    dataset.setPublished(published);
    helper.evictCache(dataset);
    save(dataset, true);
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
    updateIndices(dataset, wrappedGetDatasetVariables(dataset), false);
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll() {
    datasetIndexer.indexAll(findAllDatasets(), findAllPublishedDatasets());
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

  @Cacheable(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", key = "#variableName")
  public org.obiba.opal.web.model.Math.SummaryStatisticsDto getVariableSummary(@NotNull StudyDataset dataset,
      String variableName) throws NoSuchValueTableException, NoSuchVariableException {
    log.info("Caching variable summary {} {}", dataset.getId(), variableName);
    return getVariableValueSource(dataset, variableName).getSummary();
  }

  public Search.QueryResultDto getVariableFacet(@NotNull StudyDataset dataset, String variableName)
      throws NoSuchValueTableException, NoSuchVariableException {
    log.debug("Getting variable facet {} {}", dataset.getId(), variableName);
    return getVariableValueSource(dataset, variableName).getFacet();
  }

  public Search.QueryResultDto getFacets(@NotNull StudyDataset dataset, Search.QueryTermsDto query)
      throws NoSuchValueTableException, NoSuchVariableException {
    return getTable(dataset).getFacets(query);
  }

  public void delete(String id) {
    StudyDataset studyDataset = studyDatasetRepository.findOne(id);

    if (studyDataset == null) {
      throw NoSuchDatasetException.withId(id);
    }

    helper.evictCache(studyDataset);
    datasetIndexer.onDatasetDeleted(studyDataset);
    variableIndexer.onDatasetDeleted(studyDataset);
    studyDatasetRepository.delete(id);
  }

  @Override
  protected OpalService getOpalService() {
    return opalService;
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

  private void save(StudyDataset dataset, boolean updatePublishIndices) {
    StudyDataset saved = dataset;

    if(saved.isNew()) {
      generateId(dataset);
      saved.setId(getNextId(saved.getAcronym()));
    } else {
      saved = studyDatasetRepository.findOne(dataset.getId());

      if (saved != null) {
        BeanUtils.copyProperties(dataset, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
      } else {
        saved = dataset;
      }
    }

    Iterable<DatasetVariable> variables;

    try {
      //getting variables first to fail fast when dataset is being published
      variables = wrappedGetDatasetVariables(dataset);
    } catch(DatasourceNotAvailableException | InvalidDatasetException e) {
      if(dataset.isPublished()) {
        throw e;
      }

      if(e instanceof DatasourceNotAvailableException) {
        log.warn("Datasource not available.", e);
      }

      variables = Lists.newArrayList();
    }

    studyDatasetRepository.save(saved);
    tryUpdateIndices(saved, variables, updatePublishIndices);
  }

  private void updateIndices(StudyDataset dataset, Iterable<DatasetVariable> variables, boolean updatePublishIndices) {
    variableIndexer.onDatasetUpdated(variables);
    datasetIndexer.onDatasetUpdated(dataset);

    if (updatePublishIndices) {
      variableIndexer.onDatasetPublished(dataset, variables);
      datasetIndexer.onDatasetPublished(dataset);
    }
  }

  private void tryUpdateIndices(StudyDataset dataset, Iterable<DatasetVariable> variables, boolean updatePublishIndices) {
    try {
      updateIndices(dataset, variables, updatePublishIndices);
    } catch (Exception e) {
      log.error("Error updating indices.", e);
    }
  }

  @Component
  public static class Helper {
    private static final Logger log = LoggerFactory.getLogger(StudyDatasetService.Helper.class);

    @Inject
    StudyDatasetService service;

    @CacheEvict(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", allEntries = true, beforeInvocation = true)
    public void evictCache(StudyDataset dataset) {
      log.info("clearing dataset variables cache dataset-{}", dataset.getId());
    }
  }
}
