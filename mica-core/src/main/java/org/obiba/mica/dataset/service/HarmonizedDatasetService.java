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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.obiba.magma.*;
import org.obiba.magma.support.Disposables;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.MissingCommentException;
import org.obiba.mica.core.support.DatasetInferredAttributesCollector;
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
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.spi.tables.StudyTableSource;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
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

  private final StudyService studyService;

  private final NetworkService networkService;

  private final OpalService opalService;

  private final HarmonizationDatasetRepository harmonizationDatasetRepository;

  private final HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  private final HarmonizationStudyService harmonizationStudyService;

  private final PublishedStudyService publishedStudyService;

  private final EventBus eventBus;

  private final Helper helper;

  private final FileSystemService fileSystemService;

  private final MicaConfigService micaConfigService;

  @Inject
  public HarmonizedDatasetService(StudyService studyService, NetworkService networkService, OpalService opalService,
    HarmonizationDatasetRepository harmonizationDatasetRepository,
    HarmonizationDatasetStateRepository harmonizationDatasetStateRepository,
    HarmonizationStudyService harmonizationStudyService, PublishedStudyService publishedStudyService, EventBus eventBus,
    FileSystemService fileSystemService, MicaConfigService micaConfigService) {
    this.studyService = studyService;
    this.networkService = networkService;
    this.opalService = opalService;
    this.harmonizationDatasetRepository = harmonizationDatasetRepository;
    this.harmonizationDatasetStateRepository = harmonizationDatasetStateRepository;
    this.harmonizationStudyService = harmonizationStudyService;
    this.publishedStudyService = publishedStudyService;
    this.eventBus = eventBus;
    this.fileSystemService = fileSystemService;
    this.micaConfigService = micaConfigService;

    this.helper = new Helper(this, this.eventBus);
  }

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
    HarmonizationDataset dataset = harmonizationDatasetRepository.findById(id).orElse(null);
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
    return Lists.newArrayList(harmonizationDatasetRepository.findAllById(ids));
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
    return findPublishedDatasets(new ArrayList<>());
  }

  public List<HarmonizationDataset> findPublishedDatasets(List<String> datasetIds) {
    List<HarmonizationDatasetState> states = datasetIds != null && !datasetIds.isEmpty()
      ? harmonizationDatasetStateRepository.findByPublishedTagNotNullAndIdIn(datasetIds)
      : harmonizationDatasetStateRepository.findByPublishedTagNotNull();

    return states.stream()
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
    indexByIds(new ArrayList<>(), mustIndexVariables);
  }

  public void indexByIds(List<String> ids, boolean mustIndexVariables) {
    List<String> includeIds = ids.isEmpty() ? findAllIds() : ids;
    List<HarmonizationDataset> datasets  = findAllDatasets(includeIds);
    HashSet<HarmonizationDataset> publishedDatasets = Sets.newHashSet(findPublishedDatasets(includeIds));
    datasets.forEach(dataset -> {
      try {
        dataset.generateTableUniqueId();
        eventBus.post(new DatasetUpdatedEvent(dataset));

        if (publishedDatasets.contains(dataset)) {
          if (mustIndexVariables && publishedDatasets.contains(dataset)) {
            eventBus.post(
              new DatasetPublishedEvent(dataset, wrappedGetDatasetVariables(dataset), null, getCurrentUsername())
            );

            indexHarmonizedVariables(dataset);
          }
        }

      } catch (Exception e) {
        log.error(String.format("Error indexing dataset %s", dataset), e);
      }
    });
  }

  private void indexHarmonizedVariables(HarmonizationDataset dataset) {
    if(!dataset.getBaseStudyTables().isEmpty()) {
      dataset.getBaseStudyTables()
        .forEach(studyTable -> {
          Future<Iterable<DatasetVariable>> future = helper.asyncGetDatasetVariables(() -> getDatasetVariablesFromStudyTable(dataset, studyTable));
          try {
            Iterable<DatasetVariable> harmonizationVariables = future.get();
            eventBus.post(new DatasetPublishedEvent(dataset, null, harmonizationVariables, getCurrentUsername()));
          } catch (InterruptedException e) {
            if(e.getCause() instanceof MagmaRuntimeException) {
              throw new DatasourceNotAvailableException(e.getCause());
            }

            throw Throwables.propagate(e.getCause());
          } catch (ExecutionException e) {
            throw Throwables.propagate(e);
          }
        });
    }
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

      dataset.generateTableUniqueId();

      eventBus.post(new DatasetPublishedEvent(dataset, wrappedGetDatasetVariables(dataset), null,
        getCurrentUsername(), cascadingScope));
      indexHarmonizedVariables(dataset);
    } else {
      unPublishState(id);
      eventBus.post(new DatasetUnpublishedEvent(dataset));
    }
  }

  private void checkIsPublishable(HarmonizationDataset dataset) {
    if (dataset == null
      || dataset.getHarmonizationTable() == null
      || dataset.getHarmonizationTable().getSource() == null
      || dataset.getHarmonizationTable().getStudyId() == null) {
      throw new IllegalArgumentException("dataset.harmonization.missing-attributes");
    }

    if (!harmonizationStudyService.isPublished(dataset.getHarmonizationTable().getStudyId()))
      throw new IllegalArgumentException("dataset.harmonization.study-not-published");

    BaseStudy study = publishedStudyService.findById(dataset.getHarmonizationTable().getStudyId());
    if (study == null)
      throw NoSuchStudyException.withId(dataset.getHarmonizationTable().getStudyId());

    if (!(study instanceof HarmonizationStudy))
      throw new IllegalArgumentException("Wrong study type found");
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
    HarmonizationDataset dataset = harmonizationDatasetRepository.findById(id).orElse(null);

    if(dataset == null) {
      throw NoSuchDatasetException.withId(id);
    }

    fileSystemService.delete(FileUtils.getEntityPath(dataset));
    helper.evictCache(dataset);
    harmonizationDatasetStateRepository.deleteById(id);
    harmonizationDatasetRepository.deleteById(id);
    gitService.deleteGitRepository(dataset);
    eventBus.post(new DatasetDeletedEvent(dataset));
  }

  @Override
  protected ValueTable getValueTable(@NotNull HarmonizationDataset dataset) throws NoSuchValueTableException {
    return getStudyTableSource(dataset, dataset.getSafeHarmonizationTable()).getValueTable();
  }

  @Override
  public Iterable<DatasetVariable> getDatasetVariables(HarmonizationDataset dataset, @Nullable DatasetInferredAttributesCollector collector) throws NoSuchValueTableException {
    List<DatasetVariable> variables = StreamSupport.stream(getVariables(dataset).spliterator(), false)
      .map(input -> {
        DatasetVariable datasetVariable = new DatasetVariable(dataset, input);
        if (collector != null) {
          collector.collect(datasetVariable);
        }
        return datasetVariable;
      }).collect(toList());

    if (collector != null) {
      log.debug("Variable Attributes Collector: {}", collector.getAttributes().size());
      dataset.setInferredAttributes(collector.getAttributes());
    }
    return variables;
  }

  @Override
  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName)
    throws NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getStudyTableSource(dataset, dataset.getSafeHarmonizationTable()).getValueTable().getVariable(variableName));
  }

  public Iterable<DatasetVariable> getDatasetVariablesFromStudyTable(HarmonizationDataset dataset, BaseStudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException {
    return StreamSupport.stream(getVariables(dataset, studyTable).spliterator(), false)
      .map(input -> new DatasetVariable(dataset, input, studyTable)).collect(toList());
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, BaseStudyTable studyTable)
    throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset, getStudyTableSource(dataset, studyTable).getValueTable().getVariable(variableName));
  }

  public DatasetVariable getDatasetVariable(HarmonizationDataset dataset, String variableName, String studyId,
    String source) throws NoSuchStudyException, NoSuchValueTableException, NoSuchVariableException {
    return new DatasetVariable(dataset,
      getTable(dataset, studyId, source).getVariableValueSource(variableName).getVariable());
  }

  @Cacheable(value = "dataset-variables", cacheResolver = "datasetVariablesCacheResolver", key = "#variableName + ':' + #studyId + ':' + #source")
  public Mica.DatasetVariableAggregationDto getVariableSummary(@NotNull HarmonizationDataset dataset, String variableName, String studyId, String source) {
    for(BaseStudyTable baseTable : dataset.getBaseStudyTables()) {
      if(baseTable.isFor(studyId, source)) {
        log.info("Caching variable summary {} {} {} {}", dataset.getId(), variableName, studyId, source);
        StudyTableSource tableSource = getStudyTableSource(dataset, baseTable);
        Mica.DatasetVariableAggregationDto summary = tableSource.providesVariableSummary() ? tableSource.getVariableSummary(variableName) : null;
        Disposables.silentlyDispose(tableSource);
        return summary;
      }
    }

    throw NoSuchStudyException.withId(studyId);
  }

  public Mica.DatasetVariableContingencyDto getContingencyTable(@NotNull HarmonizationDataset dataset, @NotNull BaseStudyTable studyTable, DatasetVariable variable,
                                                                DatasetVariable crossVariable) throws NoSuchStudyException, NoSuchValueTableException {
    StudyTableSource tableSource = getStudyTableSource(dataset, studyTable);
    Mica.DatasetVariableContingencyDto results = tableSource.providesContingency() ? tableSource.getContingency(variable, crossVariable) : null;
    Disposables.silentlyDispose(tableSource);
    return results;
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
    if (!Strings.isNullOrEmpty(dataset.getId()) && micaConfigService.getConfig().isCommentsRequiredOnDocumentSave() && Strings.isNullOrEmpty(comment)) {
      throw new MissingCommentException("Due to the server configuration, comments are required when saving this document.");
    }

    boolean datasetIsNew = dataset.isNew();

    HarmonizationDataset saved = prepareSave(dataset);

    HarmonizationDatasetState harmonizationDatasetState = findEntityState(dataset, HarmonizationDatasetState::new);

    if(!datasetIsNew) ensureGitRepository(harmonizationDatasetState);

    harmonizationDatasetState.incrementRevisionsAhead();

    harmonizationDatasetStateRepository.save(harmonizationDatasetState);

    saved.setLastModifiedDate(LocalDateTime.now());

    if(!datasetIsNew) harmonizationDatasetRepository.save(saved);
    else harmonizationDatasetRepository.insert(saved);

    gitService.save(saved, comment);
    helper.getPublishedVariables(saved);
  }

  protected HarmonizationDataset prepareSave(HarmonizationDataset dataset) {
    if(dataset.isNew()) {
      dataset.setId(generateDatasetId(dataset));
      return dataset;
    } else {
      Optional<HarmonizationDataset> saved = harmonizationDatasetRepository.findById(dataset.getId());
      if(saved.isPresent()) {
        HarmonizationDataset harmonizationDataset = saved.get();
        BeanUtils.copyProperties(dataset, harmonizationDataset, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
        return harmonizationDataset;
      }
      return dataset;
    }
  }

  private Iterable<Variable> getVariables(@NotNull HarmonizationDataset dataset, BaseStudyTable studyTable)
    throws NoSuchDatasetException, NoSuchStudyException, NoSuchValueTableException {
    return getStudyTableSource(dataset, studyTable).getValueTable().getVariables();
  }

  private ValueTable getTable(@NotNull HarmonizationDataset dataset, String studyId, String source)
    throws NoSuchStudyException, NoSuchValueTableException {

    for(BaseStudyTable baseTable : dataset.getBaseStudyTables()) {
      if(baseTable.isFor(studyId, source)) {
        return getStudyTableSource(dataset, baseTable).getValueTable();
      }
    }

    throw NoSuchStudyException.withId(studyId);
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

  public static class Helper {

    private EventBus eventBus;

    private static final Logger log = LoggerFactory.getLogger(HarmonizedDatasetService.Helper.class);

    HarmonizedDatasetService service;

    public Helper(HarmonizedDatasetService service, EventBus eventBus) {
      this.service = service;
      this.eventBus = eventBus;
    }

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
          service.getVariableSummary(dataset, var.getName(), studyId, st.getSource());
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
