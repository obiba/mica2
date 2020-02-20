/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.joda.time.DateTime;
import org.obiba.magma.MagmaRuntimeException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.magma.ValueTable;
import org.obiba.magma.Variable;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.service.support.QueryTermsUtil;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
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
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class HarmonizedDatasetService extends DatasetService<HarmonizationDataset, HarmonizationDatasetState> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizedDatasetService.class);

  @Inject
  private StudyService studyService;

  @Inject
  @Lazy
  private NetworkService networkService;

  @Inject
  private OpalService opalService;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private EventBus eventBus;

  @Inject
  @Lazy
  private Helper helper;

  @Inject
  private FileSystemService fileSystemService;

  public void save(@NotNull HarmonizationDataset dataset) {
    saveInternal(dataset, null);
  }

  @Override
  public void save(@NotNull @Valid HarmonizationDataset dataset, String comment) {
    saveInternal(dataset, comment);
  }

  /**
   * Get the {@link HarmonizationDataset} from its id.
   *
   * @param id
   * @return
   * @throws NoSuchDatasetException
   */
  @Override
  @NotNull
  public HarmonizationDataset findById(@NotNull String id) throws NoSuchDatasetException {
    HarmonizationDataset dataset = harmonizationDatasetRepository.findOne(id);
    if(dataset == null) throw NoSuchDatasetException.withId(id);
    return dataset;
  }

  public List<HarmonizationDataset> findAllDatasetsByHarmonizationStudy(@NotNull String studyId) {
    Assert.isTrue(!Strings.isNullOrEmpty(studyId), "Harmonization Study ID cannot be null or empty");
    return harmonizationDatasetRepository.findByHarmonizationTableStudyId(studyId);
  }

  @Override
  public List<String> findAllIds() {
    return harmonizationDatasetRepository.findAllExistingIds().stream().map(HarmonizationDataset::getId).collect(Collectors.toList());
  }

  /**
   * Get all {@link HarmonizationDataset}s.
   *
   * @return
   */
  public List<HarmonizationDataset> findAllDatasets() {
    return harmonizationDatasetRepository.findAll();
  }

  /**
   * Get all {@link HarmonizationDataset}s.
   *
   * @return
   */
  public List<HarmonizationDataset> findAllDatasets(Iterable<String> ids) {
    return Lists.newArrayList(harmonizationDatasetRepository.findAll(ids));
  }

  /**
   * Get all {@link HarmonizationDataset}s having a reference to the given study.
   *
   * @param studyId
   * @return
   */
  public List<HarmonizationDataset> findAllDatasets(@Nullable String studyId) {
    if(Strings.isNullOrEmpty(studyId)) return findAllDatasets();
    return harmonizationDatasetRepository.findByStudyTablesStudyId(studyId);
  }

  /**
   * Get all published {@link HarmonizationDataset}s.
   *
   * @return
   */
  public List<HarmonizationDataset> findAllPublishedDatasets() {
    return harmonizationDatasetStateRepository.findByPublishedTagNotNull().stream()
      .filter(state -> { //
        return gitService.hasGitRepository(state) && !Strings.isNullOrEmpty(state.getPublishedTag()); //
      }) //
      .map(state -> gitService.readFromTag(state, state.getPublishedTag(), HarmonizationDataset.class)) //
      .map(s -> { s.getModel(); return s; }) // make sure dynamic model is initialized
      .collect(toList());
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
    indexAll(true);
  }

  public void indexAll(boolean mustIndexVariables) {
    Set<HarmonizationDataset> publishedDatasets = Sets.newHashSet(findAllPublishedDatasets());

    findAllDatasets().forEach(dataset -> {
      try {
        eventBus.post(new DatasetUpdatedEvent(dataset));

        if (publishedDatasets.contains(dataset)) {
          Map<String, List<DatasetVariable>> harmonizationVariables = mustIndexVariables && publishedDatasets.contains(dataset) ? populateHarmonizedVariablesMap(dataset) : null;
          Iterable<DatasetVariable> datasetVariables = mustIndexVariables && publishedDatasets.contains(dataset) ? wrappedGetDatasetVariables(dataset) : null;
          eventBus.post(new DatasetPublishedEvent(dataset, datasetVariables, harmonizationVariables, getCurrentUsername()));
        }

      } catch (Exception e) {
        log.error(String.format("Error indexing dataset %s", dataset), e);
      }
    });
  }

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'dataset'") })
  public void publish(@NotNull String id, boolean published) {
    publish(id, published, PublishCascadingScope.NONE);
  }

    /**
     * Apply dataset publication flag.
     *
     * @param id
     * @param published
     */
  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", key = "'dataset'") })
  public void publish(@NotNull String id, boolean published, PublishCascadingScope cascadingScope) {
    HarmonizationDataset dataset = findById(id);
    helper.evictCache(dataset);

    if(published) {
      checkIsPublishable(dataset);
      publishState(id);
      Map<String, List<DatasetVariable>> harmonizationVariables = populateHarmonizedVariablesMap(dataset);
      eventBus.post(new DatasetPublishedEvent(dataset, wrappedGetDatasetVariables(dataset), harmonizationVariables,
        getCurrentUsername(), cascadingScope));
      //helper.asyncBuildDatasetVariablesCache(dataset, harmonizationVariables);
    } else {
      unPublishState(id);
      eventBus.post(new DatasetUnpublishedEvent(dataset));
    }
  }

  private void checkIsPublishable(HarmonizationDataset dataset) {
    if (dataset == null
      || dataset.getHarmonizationTable() == null
      || dataset.getHarmonizationTable().getProject() == null
      || dataset.getHarmonizationTable().getTable() == null
      || dataset.getHarmonizationTable().getStudyId() == null
      || dataset.getHarmonizationTable().getPopulationId() == null) {
      throw new IllegalArgumentException("dataset.harmonization.missing-attributes");
    }

    if (!harmonizationStudyService.isPublished(dataset.getHarmonizationTable().getStudyId()))
      throw new IllegalArgumentException("dataset.harmonization.study-not-published");

    BaseStudy study = publishedStudyService.findById(dataset.getHarmonizationTable().getStudyId());
    if (study == null)
      throw NoSuchStudyException.withId(dataset.getHarmonizationTable().getStudyId());

    if (!(study instanceof HarmonizationStudy))
      throw new IllegalArgumentException("Wrong study type found");

    if (!isPublishedPopulation(study, dataset.getHarmonizationTable().getPopulationId()))
      throw new IllegalArgumentException("dataset.harmonization.population-not-published");
  }

  private boolean isPublishedPopulation(BaseStudy study, String populationId) {
    return study.getPopulations()
      .stream()
      .anyMatch(population -> population.getId().equals(populationId));
  }

  /**
   * Check if a dataset is published.
   *
   * @param id
   * @return
   */
  public boolean isPublished(@NotNull String id) throws NoSuchDatasetException {
    HarmonizationDatasetState state = getEntityState(id);
    return state.isPublished();
  }

  public void delete(String id) {
    HarmonizationDataset dataset = harmonizationDatasetRepository.findOne(id);

    if(dataset == null) {
      throw NoSuchDatasetException.withId(id);
    }

    fileSystemService.delete(FileUtils.getEntityPath(dataset));
    helper.evictCache(dataset);
    harmonizationDatasetStateRepository.delete(id);
    harmonizationDatasetRepository.delete(id);
    gitService.deleteGitRepository(dataset);
    eventBus.post(new DatasetDeletedEvent(dataset));
  }

  @Override
  @NotNull
  protected RestValueTable getTable(@NotNull HarmonizationDataset dataset) throws NoSuchValueTableException {
    return execute(dataset.getSafeHarmonizationTable().getProject(),
      datasource -> (RestValueTable) datasource.getValueTable(dataset.getSafeHarmonizationTable().getTable()));
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(HarmonizationDataset dataset) throws NoSuchValueTableException {
    return StreamSupport.stream(getVariables(dataset).spliterator(), false)
      .map(input -> new DatasetVariable(dataset, input)).collect(toList());
  }

  @Override
  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName)
    throws NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getVariableValueSource(dataset, variableName).getVariable());
  }

  public Iterable<DatasetVariable> getDatasetVariables(HarmonizationDataset dataset, OpalTable opalTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return StreamSupport.stream(getVariables(opalTable).spliterator(), false)
      .map(input -> new DatasetVariable(dataset, input, opalTable)).collect(toList());
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, OpalTable opalTable)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getTable(opalTable).getVariableValueSource(variableName).getVariable());
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, String studyId,
    String project, String table) throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset,
      getTable(dataset, studyId, project, table).getVariableValueSource(variableName).getVariable());
  }

  @Cacheable(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver",
    key = "#variableName + ':' + #studyId + ':' + #project + ':' + #table")
  public SummaryStatisticsWrapper getVariableSummary(@NotNull HarmonizationDataset dataset, String variableName,
    String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    log.info("Caching variable summary {} {} {} {} {}", dataset.getId(), variableName, studyId, project, table);

    return new SummaryStatisticsWrapper(
      getVariableValueSource(dataset, variableName, studyId, project, table).getSummary());
  }

  public Search.QueryResultDto getVariableFacet(@NotNull HarmonizationDataset dataset, String variableName,
    String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    log.debug("Getting variable facet {} {}", dataset.getId(), variableName);
    return getVariableValueSource(dataset, variableName, studyId, project, table).getFacet();
  }

  public Search.QueryResultDto getFacets(Search.QueryTermsDto query, OpalTable opalTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return getTable(opalTable).getFacets(query);
  }

  public Search.QueryResultDto getContingencyTable(@NotNull OpalTable opalTable, DatasetVariable variable,
                                                   DatasetVariable crossVariable) throws NoSuchStudyException, NoSuchValueTableException {
    return getFacets(QueryTermsUtil.getContingencyQuery(variable, crossVariable), opalTable);
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
  protected NetworkService getNetworkService() {
    return networkService;
  }

  @Override
  protected EventBus getEventBus() {
    return eventBus;
  }

  //
  // Private methods
  //

  @SuppressWarnings("OverlyLongMethod")
  private void saveInternal(HarmonizationDataset dataset, String comment) {
    HarmonizationDataset saved = prepareSave(dataset);

    HarmonizationDatasetState harmonizationDatasetState = findEntityState(dataset, HarmonizationDatasetState::new);

    if(!dataset.isNew()) ensureGitRepository(harmonizationDatasetState);

    harmonizationDatasetState.incrementRevisionsAhead();
    harmonizationDatasetStateRepository.save(harmonizationDatasetState);

    saved.setLastModifiedDate(DateTime.now());
    harmonizationDatasetRepository.save(saved);
    gitService.save(saved, comment);
    helper.getPublishedVariables(saved);
  }

  protected HarmonizationDataset prepareSave(HarmonizationDataset dataset) {
    if(dataset.isNew()) {
      dataset.setId(generateDatasetId(dataset));
      return dataset;
    } else {
      HarmonizationDataset saved = harmonizationDatasetRepository.findOne(dataset.getId());
      if(saved != null) {
        BeanUtils.copyProperties(dataset, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
        return saved;
      }
      return dataset;
    }
  }

  private Iterable<Variable> getVariables(OpalTable opalTable)
    throws NoSuchDatasetException, NoSuchStudyException, NoSuchValueTableException {
    return getTable(opalTable).getVariables();
  }

  private RestValueTable getTable(@NotNull OpalTable opalTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return execute(opalTable, ds -> (RestValueTable) ds.getValueTable(opalTable.getTable()));
  }

  private ValueTable getTable(@NotNull HarmonizationDataset dataset, String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException {

    for(BaseStudyTable opalTable : dataset.getBaseStudyTables()) {
      String opalTableId = studyId;
      if(opalTable.isFor(opalTableId, project, table)) {
        return getTable(opalTable);
      }
    }

    throw NoSuchStudyException.withId(studyId);
  }

  private RestValueTable.RestVariableValueSource getVariableValueSource(@NotNull HarmonizationDataset dataset,
    String variableName, String studyId, String project, String table)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    for(BaseStudyTable opalTable : dataset.getBaseStudyTables()) {
      String opalTableId = studyId;
      if(opalTable.isFor(opalTableId, project, table)) {
        return getVariableValueSource(variableName, opalTable);
      }
    }

    throw NoSuchStudyException.withId(studyId);
  }

  private RestValueTable.RestVariableValueSource getVariableValueSource(String variableName, OpalTable opalTable)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return (RestValueTable.RestVariableValueSource) getTable(opalTable).getVariableValueSource(variableName);
  }

  protected Map<String, List<DatasetVariable>> populateHarmonizedVariablesMap(HarmonizationDataset dataset) {
    Map<String, List<DatasetVariable>> map = Maps.newHashMap();

    if(!dataset.getBaseStudyTables().isEmpty()) {
      Iterable<DatasetVariable> res = dataset.getBaseStudyTables().stream()
        .map(s -> helper.asyncGetDatasetVariables(() -> getDatasetVariables(dataset, s))).map(f -> {
          try {
            return f.get();
          } catch(ExecutionException e) {
            if(e.getCause() instanceof NoSuchValueTableException) {
              return Lists.<DatasetVariable>newArrayList();  // ignore (case the study does not implement this harmonization dataset))
            }
            if(e.getCause() instanceof MagmaRuntimeException) {
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
   * @param opalTable
   * @param callback
   * @param <T>
   * @return
   */
  private <T> T execute(OpalTable opalTable, DatasourceCallback<T> callback) {
    return execute(getDatasource(opalTable), callback);
  }

  @Override
  protected EntityStateRepository<HarmonizationDatasetState> getEntityStateRepository() {
    return harmonizationDatasetStateRepository;
  }

  @Override
  protected Class<HarmonizationDataset> getType() {
    return HarmonizationDataset.class;
  }

  @Override
  public String getTypeName() {
    return "harmonized-dataset";
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
    @Inject
    private EventBus eventBus;

    private static final Logger log = LoggerFactory.getLogger(HarmonizedDatasetService.Helper.class);

    @Inject
    HarmonizedDatasetService service;

    @CacheEvict(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", allEntries = true, beforeInvocation = true)
    public void evictCache(HarmonizationDataset dataset) {
      log.info("cleared dataset variables cache dataset-{}", dataset.getId());
    }

    @Async("opalExecutor")
    public Future<Iterable<DatasetVariable>> asyncGetDatasetVariables(Supplier<Iterable<DatasetVariable>> supp) {
      log.info("Getting dataset variables asynchronously.");
      return new AsyncResult<>(supp.get());
    }

    @Async
    public void asyncBuildDatasetVariablesCache(HarmonizationDataset dataset,
      Map<String, List<DatasetVariable>> harmonizationVariables) {
      log.info("building variable summaries cache");

      dataset.getBaseStudyTables().forEach(st -> harmonizationVariables.forEach((k, v) -> v.forEach(var -> {
        try {
          String studyId = st.getStudyId();
          service.getVariableSummary(dataset, var.getName(), studyId, st.getProject(), st.getTable());
        } catch(Exception e) {
          //ignoring
        }
      })));

      log.info("done building variable summaries cache");
    }

    @Async
    public void getPublishedVariables(HarmonizationDataset dataset) {
      eventBus.post(new DatasetUpdatedEvent(dataset));
    }
  }
}
