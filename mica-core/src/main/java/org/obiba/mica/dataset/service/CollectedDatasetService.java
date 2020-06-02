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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.domain.StudyDatasetState;
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
import org.obiba.mica.study.domain.DataCollectionEvent;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.DraftStudyPopulationDceWeightChangedEvent;
import org.obiba.mica.study.service.IndividualStudyService;
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
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class CollectedDatasetService extends DatasetService<StudyDataset, StudyDatasetState> {

  private static final Logger log = LoggerFactory.getLogger(CollectedDatasetService.class);

  @Inject
  private StudyService studyService;

  @Inject
  @Lazy
  private NetworkService networkService;

  @Inject
  private OpalService opalService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private EventBus eventBus;

  @Inject
  @Lazy
  private Helper helper;

  public void save(@NotNull @Valid StudyDataset dataset) {
    saveInternal(dataset, null);
  }

  @Override
  public void save(@NotNull @Valid StudyDataset dataset, String comment) {
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

  @Override
  public List<String> findAllIds() {
    return studyDatasetRepository.findAllExistingIds().stream().map(StudyDataset::getId).collect(Collectors.toList());
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
   * Get all published {@link StudyDataset}s.
   *
   * @return
   */
  public List<StudyDataset> findAllPublishedDatasets() {
    return mapPublishedDatasets(studyDatasetStateRepository.findByPublishedTagNotNull().stream()
      .filter(state -> {
        return gitService.hasGitRepository(state) && !Strings.isNullOrEmpty(state.getPublishedTag());
      }));
  }

  public List<StudyDataset> findPublishedDatasets(List<String> datasetIds) {
    return mapPublishedDatasets(studyDatasetStateRepository.findByPublishedTagNotNullAndIdIn(datasetIds).stream()
      .filter(state -> {
        return gitService.hasGitRepository(state) && !Strings.isNullOrEmpty(state.getPublishedTag());
      }));
  }

  public List<StudyDataset> findAllRequireIndexing() {
    List<String> ids = findDatasetStatesRequireIndexing(new ArrayList<>())
      .stream()
      .map(StudyDatasetState::getId)
      .collect(toList());

    return findAllDatasets(ids);
  }

  private List<StudyDatasetState> findDatasetStatesRequireIndexing(List<String> ids) {
    return ids.isEmpty()
      ? studyDatasetStateRepository.findAllByRequireIndexingIsTrue()
      : studyDatasetStateRepository.findAllByRequireIndexingIsTrueAndIdIn(ids);
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

  public List<StudyDataset> findAllDatasets(Iterable<String> ids) {
    return Lists.newArrayList(studyDatasetRepository.findAll(ids));
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
    StudyDataset dataset = findById(id);
    helper.evictCache(dataset);

    if(published) {
      checkIsPublishable(dataset);
      Iterable<DatasetVariable> variables = wrappedGetDatasetVariables(dataset);
      publishState(id);
      prepareForIndex(dataset);
      eventBus.post(new DatasetPublishedEvent(dataset, variables, getCurrentUsername(), cascadingScope));
      //helper.asyncBuildDatasetVariablesCache(dataset, variables);
    } else {
      unPublishState(id);
      eventBus.post(new DatasetUnpublishedEvent(dataset));
    }
  }

  @Override
  protected StudyDatasetState publishStateInternal(String id) throws NoSuchEntityException {
    StudyDatasetState studyDatasetState = super.publishStateInternal(id);
    studyDatasetState.setRequireIndexing(false);
    return studyDatasetState;
  }

  @Override
  protected StudyDatasetState unPublishStateInternal(String id) {
    StudyDatasetState studyDatasetState = super.unPublishStateInternal(id);
    studyDatasetState.setRequireIndexing(false);
    return studyDatasetState;
  }

  private List<StudyDataset> mapPublishedDatasets(Stream<StudyDatasetState> studyDatasetStateStream) {
    return studyDatasetStateStream
      .map(state -> gitService.readFromTag(state, state.getPublishedTag(), StudyDataset.class))
      .map(ds -> { ds.getModel(); return ds; }) // make sure dynamic model is initialized
      .collect(toList());
  }

  private void checkIsPublishable(StudyDataset dataset) {
    if (!dataset.hasStudyTable())
      return;

    if (!individualStudyService.isPublished(dataset.getStudyTable().getStudyId()))
      throw new IllegalArgumentException("dataset.collection.study-not-published");

    BaseStudy study = publishedStudyService.findById(dataset.getStudyTable().getStudyId());
    if (study == null)
      throw NoSuchStudyException.withId(dataset.getStudyTable().getStudyId());

    if(!(study instanceof Study))
      throw new IllegalArgumentException("Wrong study type found");

    if (!isPublishedPopulation(study, dataset.getStudyTable().getPopulationId()))
      throw new IllegalArgumentException("dataset.collection.population-not-published");
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
    StudyDatasetState state = getEntityState(id);
    return state.isPublished();
  }

  /**
   * Index the dataset
   *
   * @param id
   */
  public void index(@NotNull String id) {
    StudyDataset dataset = findById(id);
    prepareForIndex(dataset);
    eventBus.post(new DatasetUpdatedEvent(dataset));
  }

  private void prepareForIndex(StudyDataset dataset) {
    if (dataset.hasStudyTable()) {
      StudyTable studyTable = dataset.getStudyTable();

      BaseStudy study = publishedStudyService.findById(dataset.getStudyTable().getStudyId());
      if (study != null) {
        Population population = study.findPopulation(studyTable.getPopulationId());
        if(population != null) {
          studyTable.setPopulationWeight(population.getWeight());
          DataCollectionEvent dataCollectionEvent = population.findDataCollectionEvent(studyTable.getDataCollectionEventId());

          if(dataCollectionEvent != null) {
            studyTable.setDataCollectionEventWeight(dataCollectionEvent.getWeight());
          }
        }
      }
    }
  }

  /**
   * Index or re-index all datasets with their variables.
   */
  public void indexAll() {
    indexAll(true);
  }

  public void indexAll(boolean mustIndexVariables) {
    indexByIds(new ArrayList<>(), mustIndexVariables);
  }

  public void indexByIds(List<String> ids, boolean mustIndexVariables) {
    List<StudyDatasetState> datasetsRequireIndexing = findDatasetStatesRequireIndexing(ids);
    List<String> excludeIds = datasetsRequireIndexing.stream().map(StudyDatasetState::getId).collect(toList());

    if (excludeIds.isEmpty()) {
      indexDatasets(Sets.newHashSet(findAllPublishedDatasets()), findAllDatasets(), mustIndexVariables);
    } else {
      // To make sure requireIndexing is properly maintained set/cleared, index these separately
      indexRequireIndexing(datasetsRequireIndexing);

      List<StudyDataset> datasets = studyDatasetRepository.findByIdNotIn(excludeIds);
      HashSet<StudyDataset> publishedDatasets =
        Sets.newHashSet(findPublishedDatasets(datasets.stream().map(Dataset::getId).collect(toList())));
      indexDatasets(publishedDatasets, datasets, mustIndexVariables);
    }
  }

  private void indexRequireIndexing(List<StudyDatasetState> states) {
    if (!states.isEmpty()) {
      // to minimize discrepancy clear flag here
      List<String> datasetIds = states.stream()
        .map(state -> {
          state.setRequireIndexing(false);
          return state.getId();
        })
        .collect(toList());
      studyDatasetStateRepository.save(states);

      // index datasets
      List<StudyDataset> datasets = findAllDatasets(datasetIds);
      HashSet<StudyDataset> publishedDatasets =
        Sets.newHashSet(findPublishedDatasets(datasets.stream().map(AbstractGitPersistable::getId).collect(toList())));

      // reset the flag for datasets that were not processed
      Collection<StudyDataset> unprocessedDatasets = indexDatasets(publishedDatasets, datasets, true);

      if (!unprocessedDatasets.isEmpty()) {
        datasetIds = unprocessedDatasets.stream().map(Dataset::getId).collect(toList());
        // to minimize discrepancy get the states from repository again
        states = studyDatasetStateRepository.findByPublishedTagNotNullAndIdIn(datasetIds)
          .stream()
          .peek(state -> state.setRequireIndexing(true))
          .collect(toList());
        studyDatasetStateRepository.save(states);
      }
    }
  }

  public List<DatasetVariable> processVariablesForStudyDataset(StudyDataset dataset, Iterable<DatasetVariable> variables) {
    if (!dataset.hasStudyTable()) {
      return Lists.newArrayList(variables);
    }

    StudyTable studyTable = dataset.getStudyTable();

    BaseStudy study = studyService.findStudy(dataset.getStudyTable().getStudyId());
    Population population = study.findPopulation(studyTable.getPopulationId());

    if (population == null) {
      return Lists.newArrayList(variables);
    }

    int populationWeight = population.getWeight();

    DataCollectionEvent dataCollectionEvent = population
      .findDataCollectionEvent(studyTable.getDataCollectionEventId());

    if (dataCollectionEvent == null) {
      return Lists.newArrayList(variables);
    }

    int dataCollectionEventWeight =  dataCollectionEvent.getWeight();

    return StreamSupport.stream(variables.spliterator(), false).map(datasetVariable -> {
      datasetVariable.setPopulationWeight(populationWeight);
      datasetVariable.setDataCollectionEventWeight(dataCollectionEventWeight);

      return datasetVariable;
    }).collect(toList());
  }

  @Override
  @NotNull
  protected RestValueTable getTable(@NotNull StudyDataset dataset) throws NoSuchValueTableException {
    StudyTable studyTable = dataset.getSafeStudyTable();
    return execute(studyTable, datasource -> (RestValueTable) datasource.getValueTable(studyTable.getTable()));
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(StudyDataset dataset) {
    if (dataset.hasStudyTable()) {
      return StreamSupport.stream(getVariables(dataset).spliterator(), false)
        .map(input -> new DatasetVariable(dataset, input)).collect(toList());
    }
    return Lists.newArrayList();
  }

  @Override
  public DatasetVariable getDatasetVariable(StudyDataset dataset, String variableName)
    throws NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getVariableValueSource(dataset, variableName).getVariable());
  }

  @Cacheable(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", key = "#variableName")
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

    fileSystemService.delete(FileUtils.getEntityPath(dataset));
    helper.evictCache(dataset);
    studyDatasetRepository.delete(id);
    studyDatasetStateRepository.delete(id);
    gitService.deleteGitRepository(dataset);
    eventBus.post(new DatasetDeletedEvent(dataset));
  }

  /**
   * Upon an order change of a population or DCE, corresponding published datasets must be indexed to preserve the
   * correct of their variables.
   *
   * @param event
   */
  @Async
  @Subscribe
  public void studyPopulationDceWeightChanged(DraftStudyPopulationDceWeightChangedEvent event) {
    BaseStudy study = event.getPersistable();
    if (study != null) {
      List<String> datasetIds = findAllDatasets(study.getId()).stream().map(Dataset::getId).collect(toList());

      // Only published datasets to require indexing.
      List<StudyDatasetState> states = studyDatasetStateRepository.findByPublishedTagNotNullAndIdIn(datasetIds)
        .stream()
        .peek(state -> state.setRequireIndexing(true))
        .collect(toList());
      studyDatasetStateRepository.save(states);
    }
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

    StudyDatasetState studyDatasetState = findEntityState(dataset, StudyDatasetState::new);

    if(!dataset.isNew()) ensureGitRepository(studyDatasetState);

    studyDatasetState.incrementRevisionsAhead();
    studyDatasetStateRepository.save(studyDatasetState);

    saved.setLastModifiedDate(DateTime.now());
    studyDatasetRepository.save(saved);
    gitService.save(saved, comment);
    eventBus.post(new DatasetUpdatedEvent(saved));
  }

  private StudyDataset prepareSave(StudyDataset dataset) {
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

  private Collection<StudyDataset> indexDatasets(Set<StudyDataset> publishedDatasets, List<StudyDataset> datasets, boolean mustIndexVariables) {
    List<StudyDataset> unprocessedDatasets = new ArrayList<>();

    datasets
      .forEach(dataset -> {
        try {
          eventBus.post(new DatasetUpdatedEvent(dataset));

          if (publishedDatasets.contains(dataset)) {
            prepareForIndex(dataset);
            Iterable<DatasetVariable> variables = mustIndexVariables && publishedDatasets.contains(dataset) ? wrappedGetDatasetVariables(dataset) : null;
            eventBus.post(new DatasetPublishedEvent(dataset, variables, getCurrentUsername()));
          }
        } catch (Exception e) {
          unprocessedDatasets.add(dataset);
          log.error(String.format("Error indexing dataset %s", dataset), e);
        }
      });

    return unprocessedDatasets;
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
    return "collected-dataset";
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
    CollectedDatasetService service;

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
