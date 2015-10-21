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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.magma.Variable;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.GitPersistable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.event.IndexHarmonizationDatasetsEvent;
import org.obiba.mica.dataset.service.support.QueryTermsUtil;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.NoSuchStudyException;
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
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class HarmonizationDatasetService extends DatasetService<HarmonizationDataset, HarmonizationDatasetState> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationDatasetService.class);

  @Inject
  private StudyService studyService;

  @Inject
  private OpalService opalService;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private DatasetIndexer datasetIndexer;

  @Inject
  private VariableIndexer variableIndexer;

  @Inject
  @Lazy
  private Helper helper;

  public void save(@NotNull HarmonizationDataset dataset) {
    save(dataset, false, null);
  }

  @Override
  public void save(@NotNull HarmonizationDataset dataset, String comment) {
    save(dataset, false, comment);
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
  public List<HarmonizationDataset> findAllDatasets(@Nullable String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllDatasets();
    return harmonizationDatasetRepository.findByStudyTablesStudyId(studyId);
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s.
   *
   * @return
   */
  public List<HarmonizationDataset> findAllPublishedDatasets() {
    Set<String> publishedIds = harmonizationDatasetStateRepository.findByPublishedTagNotNull().stream().map(HarmonizationDatasetState::getId).collect(toSet());

    return Lists.newArrayList(harmonizationDatasetRepository.findAll(publishedIds));
  }

  /**
   * Get all published {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizationDataset> findAllPublishedDatasets(String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllPublishedDatasets();

    Set<String> publishedIds = harmonizationDatasetStateRepository.findByPublishedTagNotNull().stream().map(HarmonizationDatasetState::getId).collect(toSet());

    return harmonizationDatasetRepository.findByStudyTablesStudyId(studyId).stream()
      .filter(s -> publishedIds.contains(s.getId())).collect(toList());
  }

  /**
   * Index the dataset and associated variables.
   *
   * @param id
   */
  public void index(@NotNull String id) {
    HarmonizationDataset dataset = findById(id);
    updateIndices(dataset, wrappedGetDatasetVariables(dataset), populateHarmonizedVariablesMap(dataset), false);
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll(boolean includeVariables) {
    List<HarmonizationDataset> allDatasets = findAllDatasets();
    List<HarmonizationDataset> publishedDatasets = findAllPublishedDatasets();

    if(!includeVariables) {
      datasetIndexer.indexAll(allDatasets, publishedDatasets);
    } else {
      allDatasets.forEach(
        dataset -> updateIndices(dataset, wrappedGetDatasetVariables(dataset), populateHarmonizedVariablesMap(dataset),
          true));
    }

    getEventBus().post(new IndexHarmonizationDatasetsEvent());
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
    HarmonizationDataset dataset = findById(id);
    helper.evictCache(dataset);

    if (published) publishState(id);
    else unPublish(id);

    updateIndices(dataset, wrappedGetDatasetVariables(dataset), populateHarmonizedVariablesMap(dataset), true);
  }

  /**
   * Check if a dataset is published.
   *
   * @param id
   * @return
   */
  public boolean isPublished(@NotNull String id) throws NoSuchDatasetException {
    HarmonizationDatasetState state = findStateById(id);

    return state.isPublished();
  }

  public void delete(String id) {
    HarmonizationDataset dataset = harmonizationDatasetRepository.findOne(id);

    if(dataset == null) {
      throw NoSuchDatasetException.withId(id);
    }

    helper.evictCache(dataset);
    datasetIndexer.onDatasetDeleted(dataset);
    variableIndexer.onDatasetDeleted(dataset);
    harmonizationDatasetRepository.delete(id);
    gitService.deleteGitRepository(dataset);
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

  public Iterable<DatasetVariable> getDatasetVariables(HarmonizationDataset dataset, StudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return Iterables.transform(getVariables(studyTable), input -> new DatasetVariable(dataset, input, studyTable));
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, StudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getTable(studyTable).getVariableValueSource(variableName).getVariable());
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, String studyId,
    String project, String table) throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset,
      getTable(dataset, studyId, project, table).getVariableValueSource(variableName).getVariable());
  }

  @Cacheable(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver",
    key = "#variableName + ':' + #studyId + ':' + #project + ':' + #table")
  public SummaryStatisticsWrapper getVariableSummary(
    @NotNull HarmonizationDataset dataset, String variableName, String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    log.info("Caching variable summary {} {} {} {} {}", dataset.getId(), variableName, studyId, project, table);

    return new SummaryStatisticsWrapper(getVariableValueSource(dataset, variableName, studyId, project, table).getSummary());
  }

  public Search.QueryResultDto getVariableFacet(@NotNull HarmonizationDataset dataset, String variableName,
    String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    log.debug("Getting variable facet {} {}", dataset.getId(), variableName);
    return getVariableValueSource(dataset, variableName, studyId, project, table).getFacet();
  }

  public Search.QueryResultDto getFacets(Search.QueryTermsDto query, StudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return getTable(studyTable).getFacets(query);
  }

  public Search.QueryResultDto getContingencyTable(@NotNull StudyTable studyTable,
    DatasetVariable variable, DatasetVariable crossVariable) throws NoSuchStudyException, NoSuchValueTableException {
    return getFacets(QueryTermsUtil.getContingencyQuery(variable, crossVariable), studyTable);
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

  private void save(HarmonizationDataset dataset, boolean updatePublishIndices, String comment) {
    HarmonizationDataset saved = dataset;

    if(saved.isNew()) {
      saved.setId(generateDatasetId(dataset));
    } else {
      saved = harmonizationDatasetRepository.findOne(dataset.getId());

      if(saved != null) {
        BeanUtils.copyProperties(dataset, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
      } else {
        saved = dataset;
      }
    }

    Iterable<DatasetVariable> variables;
    Map<String, List<DatasetVariable>> harmonizationVariables;

    try {
      //getting variables first to fail fast when dataset is being published
      variables = wrappedGetDatasetVariables(dataset);
      harmonizationVariables = populateHarmonizedVariablesMap(dataset);
    } catch(DatasourceNotAvailableException | InvalidDatasetException e) {
      if(dataset.isPublished()) {
        throw e;
      }

      if(e instanceof DatasourceNotAvailableException) {
        log.warn("Datasource not available.", e);
      }

      variables = Lists.newArrayList();
      harmonizationVariables = Maps.newHashMap();
    }

    HarmonizationDatasetState harmonizationDatasetState = findEntityState(dataset, () -> {
      HarmonizationDatasetState defaultState = new HarmonizationDatasetState();
      return defaultState;
    });

    if(!dataset.isNew()) ensureGitRepository(harmonizationDatasetState);

    harmonizationDatasetState.incrementRevisionsAhead();
    harmonizationDatasetStateRepository.save(harmonizationDatasetState);

    saved.setLastModifiedDate(DateTime.now());
    harmonizationDatasetRepository.save(saved);
    tryUpdateIndices(saved, variables, harmonizationVariables, updatePublishIndices);

    gitService.save(saved, comment);
  }

  private void updateIndices(HarmonizationDataset dataset, Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizationVariables, boolean updatePublishIndices) {
    variableIndexer.onDatasetUpdated(variables, harmonizationVariables);
    datasetIndexer.onDatasetUpdated(dataset);

    if(updatePublishIndices) {
      variableIndexer.onDatasetPublished(dataset, variables, harmonizationVariables);
      datasetIndexer.onDatasetPublished(dataset);

      if (dataset.isPublished()) {
        helper.asyncBuildDatasetVariablesCache(dataset, harmonizationVariables);
      }
    }

    log.info("done updating indices");
  }

  private void tryUpdateIndices(HarmonizationDataset dataset, Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizationVariables, boolean updatePublishIndices) {

    try {
      updateIndices(dataset, variables, harmonizationVariables, updatePublishIndices);
    } catch(Exception e) {
      log.error("Error updating indices.", e);
    }
  }

  private Iterable<Variable> getVariables(StudyTable studyTable)
    throws NoSuchDatasetException, NoSuchStudyException, NoSuchValueTableException {
    return getTable(studyTable).getVariables();
  }

  private RestValueTable getTable(@NotNull StudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return execute(studyTable, ds -> (RestValueTable) ds.getValueTable(studyTable.getTable()));
  }

  private RestValueTable getTable(@NotNull HarmonizationDataset dataset, String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException {
    for(StudyTable studyTable : dataset.getStudyTables()) {
      if(studyTable.isFor(studyId, project, table)) {
        return getTable(studyTable);
      }
    }
    throw NoSuchStudyException.withId(studyId);
  }

  private RestValueTable.RestVariableValueSource getVariableValueSource(@NotNull HarmonizationDataset dataset,
    String variableName, String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    for(StudyTable studyTable : dataset.getStudyTables()) {
      if(studyTable.isFor(studyId, project, table)) {
        return getVariableValueSource(variableName, studyTable);
      }
    }
    throw NoSuchStudyException.withId(studyId);
  }

  private RestValueTable.RestVariableValueSource getVariableValueSource(String variableName, StudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return (RestValueTable.RestVariableValueSource) getTable(studyTable).getVariableValueSource(variableName);
  }

  protected Map<String, List<DatasetVariable>> populateHarmonizedVariablesMap(HarmonizationDataset dataset) {
    Map<String, List<DatasetVariable>> map = Maps.newHashMap();

    if(!dataset.getStudyTables().isEmpty()) {

      Iterable<DatasetVariable> res = dataset.getStudyTables().stream()
        .map(s -> helper.asyncGetDatasetVariables(() -> getDatasetVariables(dataset, s))).map(f -> {
          try {
            return f.get();
          } catch(ExecutionException e) {
            if(e.getCause() instanceof NoSuchValueTableException) {
              return Lists
                .<DatasetVariable>newArrayList();  // ignore (case the study does not implement this harmonization dataset))
            } else if(e.getCause() instanceof MagmaRuntimeException) {
              throw new DatasourceNotAvailableException(e.getCause());
            }

            throw Throwables.propagate(e.getCause());
          } catch(InterruptedException ie) {
            throw Throwables.propagate(ie);
          }
        }).reduce(Iterables::concat).get();

      for(DatasetVariable variable : res) {
        if(!map.containsKey(variable.getParentId())) {
          map.put(variable.getParentId(), Lists.newArrayList());
        }

        map.get(variable.getParentId()).add(variable);
      }
    }

    return map;
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

  @Override
  protected EntityStateRepository<HarmonizationDatasetState> getEntityStateRepository() {
    return harmonizationDatasetStateRepository;
  }

  @Override
  protected GitPersistable unpublish(HarmonizationDatasetState gitPersistable) {
    unpublishState(gitPersistable);
    HarmonizationDataset dataset = harmonizationDatasetRepository.findOne(gitPersistable.getId());

    if(dataset != null) {
      dataset.setPublished(gitPersistable.isPublished());
      updateIndices(dataset, wrappedGetDatasetVariables(dataset), populateHarmonizedVariablesMap(dataset), true);
    }

    return dataset;
  }

  @Override
  protected Class<HarmonizationDataset> getType() {
    return HarmonizationDataset.class;
  }

  @Override
  public HarmonizationDataset findDraft(@NotNull String id) throws NoSuchEntityException {
    return findById(id);
  }

  @Override
  protected String generateId(@NotNull HarmonizationDataset gitPersistable) {
    return generateDatasetId(gitPersistable);
  }

  @Component
  public static class Helper {

    private static final Logger log = LoggerFactory.getLogger(HarmonizationDatasetService.Helper.class);

    @Inject
    HarmonizationDatasetService service;

    @CacheEvict(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", allEntries = true, beforeInvocation = true)
    public void evictCache(HarmonizationDataset dataset) {
      log.info("cleared dataset variables cache dataset-{}", dataset.getId());
    }

    @Async
    public Future<Iterable<DatasetVariable>> asyncGetDatasetVariables(Supplier<Iterable<DatasetVariable>> supp) {
      log.info("Getting dataset variables asynchronously.");
      return new AsyncResult<>(supp.get());
    }

    @Async
    public void asyncBuildDatasetVariablesCache(HarmonizationDataset dataset,
      Map<String, List<DatasetVariable>> harmonizationVariables) {
      log.info("building variable summaries cache");

      dataset.getStudyTables().forEach(st -> harmonizationVariables.forEach((k, v) -> v.forEach(var -> {
            try {
              service.getVariableSummary(dataset, var.getName(), st.getStudyId(), st.getProject(), st.getTable());
            } catch(Exception e) {
              //ignoring
            }
          })));

      log.info("done building variable summaries cache");
    }
  }
}
