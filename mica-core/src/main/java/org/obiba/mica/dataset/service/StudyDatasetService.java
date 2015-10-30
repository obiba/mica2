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
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.service.support.QueryTermsUtil;
import org.obiba.mica.micaConfig.service.OpalService;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class StudyDatasetService extends DatasetService<StudyDataset, StudyDatasetState> {

  private static final Logger log = LoggerFactory.getLogger(StudyDatasetService.class);

  @Inject
  private StudyService studyService;

  @Inject
  private OpalService opalService;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  @Lazy
  private Helper helper;

  public void save(@NotNull StudyDataset dataset) {
    saveInternal(dataset, null);
  }

  @Override
  public void save(StudyDataset dataset, String comment) {
    saveInternal(dataset, comment);
  }

  /**
   * Get the {@link StudyDataset} fron its id
   *
   * @param id
   * @return
   * @throws NoSuchDatasetException
   */
  @Override
  @NotNull
  public StudyDataset findById(@NotNull String id) throws NoSuchDatasetException {
    StudyDataset dataset = studyDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  /**
   * Get all {@link StudyDataset}s.
   *
   * @return
   */
  public List<StudyDataset> findAllDatasets() {
    return studyDatasetRepository.findAll();
  }

  /**
   * Get all {@link StudyDataset}s of a study.
   *
   * @param studyId
   * @return
   */
  public List<StudyDataset> findAllDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllDatasets();
    return studyDatasetRepository.findByStudyTableStudyId(studyId);
  }

  /**
   * Apply dataset publication flag.
   *
   * @param id
   * @param published
   */
  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'dataset'") })
  public void publish(@NotNull String id, boolean published) {
    StudyDataset dataset = findById(id);
    helper.evictCache(dataset);

    if(published) {
      Iterable<DatasetVariable> variables = wrappedGetDatasetVariables(dataset);
      publishState(id);
      eventBus.post(new DatasetPublishedEvent(dataset, variables, getCurrentUsername()));
      helper.asyncBuildDatasetVariablesCache(dataset, variables);
    } else {
      unPublishState(id);
      eventBus.post(new DatasetUnpublishedEvent(dataset));
    }
  }

  /**
   * Check if a dataset is published.
   *
   * @param id
   * @return
   */
  public boolean isPublished(@NotNull String id) throws NoSuchDatasetException {
    StudyDatasetState state = getEntityState(id);
    return state.isPublished();
  }

  /**
   * Index the dataset and associated variables.
   *
   * @param id
   */
  public void index(@NotNull String id) {
    StudyDataset dataset = findById(id);
    eventBus.post(new DatasetUpdatedEvent(dataset, wrappedGetDatasetVariables(dataset)));
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll() {
    findAllDatasets()
      .forEach(dataset -> eventBus.post(new DatasetUpdatedEvent(dataset, wrappedGetDatasetVariables(dataset))));
  }

  @Override
  @NotNull
  protected RestValueTable getTable(@NotNull StudyDataset dataset) throws NoSuchValueTableException {
    StudyTable studyTable = dataset.getStudyTable();
    return execute(studyTable, datasource -> (RestValueTable) datasource.getValueTable(studyTable.getTable()));
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(StudyDataset dataset) {
    return StreamSupport.stream(getVariables(dataset).spliterator(), false)
      .map(input -> new DatasetVariable(dataset, input)).collect(toList());
  }

  @Override
  public DatasetVariable getDatasetVariable(StudyDataset dataset, String variableName)
    throws NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getVariableValueSource(dataset, variableName).getVariable());
  }

  @Cacheable(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", key = "#variableName  + ':' + #dataset.getStudyTable().getStudyId() + ':' + #dataset.getStudyTable().getProject() + ':' + #dataset.getStudyTable().getTable()")
  public SummaryStatisticsWrapper getVariableSummary(@NotNull StudyDataset dataset, String variableName)
    throws NoSuchValueTableException, NoSuchVariableException {
    log.info("Caching variable summary {} {}", dataset.getId(), variableName);
    return new SummaryStatisticsWrapper(getVariableValueSource(dataset, variableName).getSummary());
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

  public Search.QueryResultDto getContingencyTable(@NotNull StudyDataset dataset, DatasetVariable variable,
    DatasetVariable crossVariable) throws NoSuchValueTableException, NoSuchVariableException {
    return getFacets(dataset, QueryTermsUtil.getContingencyQuery(variable, crossVariable));
  }

  public void delete(String id) {
    StudyDataset dataset = studyDatasetRepository.findOne(id);

    if(dataset == null) {
      throw NoSuchDatasetException.withId(id);
    }

    helper.evictCache(dataset);
    studyDatasetRepository.delete(id);
    gitService.deleteGitRepository(dataset);
    eventBus.post(new DatasetDeletedEvent(dataset));
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

  private void saveInternal(StudyDataset dataset, String comment) {
    StudyDataset saved = prepareSave(dataset);

    Iterable<DatasetVariable> variables;

    try {
      //getting variables first to fail fast when dataset is being published
      variables = wrappedGetDatasetVariables(dataset);
    } catch(DatasourceNotAvailableException | InvalidDatasetException e) {
      if(e instanceof DatasourceNotAvailableException) {
        log.warn("Datasource not available.", e);
      }
      variables = Lists.newArrayList();
    }

    StudyDatasetState studyDatasetState = findEntityState(dataset, StudyDatasetState::new);

    if(!dataset.isNew()) ensureGitRepository(studyDatasetState);

    studyDatasetState.incrementRevisionsAhead();
    studyDatasetStateRepository.save(studyDatasetState);

    saved.setLastModifiedDate(DateTime.now());
    studyDatasetRepository.save(saved);
    gitService.save(saved, comment);
    eventBus.post(new DatasetUpdatedEvent(saved, variables, null));
  }

  protected StudyDataset prepareSave(StudyDataset dataset) {
    if(dataset.isNew()) {
      dataset.setId(generateDatasetId(dataset));
      return dataset;
    } else {
      StudyDataset saved = studyDatasetRepository.findOne(dataset.getId());
      if(saved != null) {
        BeanUtils.copyProperties(dataset, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
        return saved;
      }
      return dataset;
    }
  }

  @Override
  protected EntityStateRepository<StudyDatasetState> getEntityStateRepository() {
    return studyDatasetStateRepository;
  }

  @Override
  protected Class<StudyDataset> getType() {
    return StudyDataset.class;
  }

  @Override
  public String getTypeName() {
    return "study-dataset";
  }

  @Override
  public StudyDataset findDraft(@NotNull String id) throws NoSuchEntityException {
    return findById(id);
  }

  @Override
  protected String generateId(@NotNull StudyDataset dataset) {
    return generateDatasetId(dataset);
  }

  @Component
  public static class Helper {
    private static final Logger log = LoggerFactory.getLogger(Helper.class);

    @Inject
    StudyDatasetService service;

    @CacheEvict(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", allEntries = true, beforeInvocation = true)
    public void evictCache(StudyDataset dataset) {
      log.info("clearing dataset variables cache dataset-{}", dataset.getId());
    }

    @Async
    public void asyncBuildDatasetVariablesCache(StudyDataset dataset, Iterable<DatasetVariable> variables) {
      log.info("building variable summaries cache");

      variables.forEach(var -> {
        try {
          service.getVariableSummary(dataset, var.getName());
        } catch(Exception e) {
          //ignoring
        }
      });


      log.info("done building variable summaries cache");
    }

  }
}
