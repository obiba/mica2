package org.obiba.mica.study.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.study.ConstraintException;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import static java.util.stream.Collectors.toList;
import static org.obiba.mica.core.domain.RevisionStatus.DRAFT;

@Service
@Validated
public class StudyService implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log = LoggerFactory.getLogger(StudyService.class);

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private GitService gitService;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private EventBus eventBus;

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid Study study) {
    boolean newStudy = study.isNew();
    StudyState studyState = findStudyState(study);
    log.info("Saving study: {}", study.getId());
    if (!newStudy) ensureStudyGitRepository(studyState);
    gitService.save(study);

    studyState.setName(study.getName());
    studyState.incrementRevisionsAhead();
    studyStateRepository.save(studyState);
    studyRepository.save(study);

    eventBus.post(new DraftStudyUpdatedEvent(study));
  }

  @NotNull
  private StudyState findStudyState(Study study) {
    StudyState studyState;

    if(study.isNew()) {
      studyState = new StudyState();
      studyState.setName(study.getName());
      studyState.setId(generateId(study));
      studyStateRepository.save(studyState);
      study.setId(studyState.getId());
      return studyState;
    }

    studyState = studyStateRepository.findOne(study.getId());
    if(studyState == null) {
      studyState = new StudyState();
      studyState.setName(study.getName());
      studyState.setId(study.getId());
      studyStateRepository.save(studyState);
    }

    return studyState;
  }

  @NotNull
  public StudyState findStateById(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = getStudyStateInternal(id);
    ensureStudyGitRepositoryAndSave(studyState);
    return studyState;
  }

  private StudyState getStudyStateInternal(String id) {
    StudyState studyState = studyStateRepository.findOne(id);
    if(studyState == null) throw NoSuchStudyException.withId(id);
    return studyState;
  }

  @NotNull
  @Cacheable(value = "studies-draft", key = "#id")
  public Study findDraftStudy(@NotNull String id) throws NoSuchStudyException {
    // ensure study exists
    getStudyStateInternal(id);
    return studyRepository.findOne(id);
  }

  @NotNull
  public Study findStudy(@NotNull String id) throws NoSuchStudyException {
    // ensure study exists
    StudyState studyState = getStudyStateInternal(id);
    Study study = null;
    if (studyState.isPublished()) {
      study = publishedStudyService.findById(id);
      if (study == null) {
        // correct the discrepancy between state and the published index
        study = studyRepository.findOne(id);
        eventBus.post(new StudyPublishedEvent(study));
      }
    }

    return study == null ? studyRepository.findOne(id) : study;
  }

  @Nullable
  @Cacheable(value = "studies-published", key = "#id")
  public Study findPublishedStudyByTag(@NotNull String id, @NotNull String tag) throws NoSuchStudyException {
    if (gitService.hasGitRepository(id)) return gitService.readFromTag(id, tag, Study.class);
    throw NoSuchStudyException.withId(id);
  }

  @Cacheable(value = "studies-published", key = "#id")
  public Study findPublishedStudy(@NotNull String id) throws NoSuchStudyException {
    return publishedStudyService.findById(id);
  }

  public boolean isPublished(@NotNull String id) throws NoSuchStudyException {
    return findStateById(id).isPublished();
  }

  public List<StudyState> findAllStates() {
    return studyStateRepository.findAll();
  }

  public List<StudyState> findAllStates(String... ids) {
    return Lists.newArrayList(studyStateRepository.findAll(Arrays.asList(ids)));
  }

  public List<Study> findAllDraftStudies() {
    return studyRepository.findAll();
  }

  public List<Study> findAllDraftStudies(Iterable<String> ids) {
    return Lists.newArrayList(studyRepository.findAll(ids));
  }

  public List<StudyState> findPublishedStates() {
    return studyStateRepository.findByPublishedTagNotNull();
  }

  /**
   * Publish current revision (HEAD)
   *
   * @param id
   * @return
   * @throws NoSuchStudyException
   */
  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id") })
  public StudyState publish(@NotNull String id) throws NoSuchStudyException {
    log.info("Publish study: {}", id);
    StudyState studyState = findStateById(id);
    studyState.setRevisionStatus(DRAFT);
    studyState.setPublishedTag(gitService.tag(id));
    studyState.resetRevisionsAhead();
    studyStateRepository.save(studyState);
    eventBus.post(new StudyPublishedEvent(studyRepository.findOne(id)));
    return studyState;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
    log.info("Gather published and draft studies to be indexed");
    List<Study> publishedStudies = findPublishedStates().stream() //
      .filter(studyState -> { //
          return gitService.hasGitRepository(studyState.getId()) && !Strings.isNullOrEmpty(studyState.getPublishedTag()); //
        }) //
      .map(studyState -> gitService.readFromTag(studyState.getId(), studyState.getPublishedTag(), Study.class)) //
      .collect(toList()); //

    eventBus.post(new IndexStudiesEvent(publishedStudies, findAllDraftStudies()));
  }

  @Caching(evict = {
    @CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id")})
  public void delete(@NotNull String id) {
    Study study = studyRepository.findOne(id);

    if(study == null) {
      throw NoSuchStudyException.withId(id);
    }

    checkStudyConstraints(study);

    gitService.deleteGitRepository(id);
    eventBus.post(new StudyDeletedEvent(study));
    studyStateRepository.delete(id);
    studyRepository.delete(id);
  }

  //
  // Private methods
  //

  private void checkStudyConstraints(Study study) {
    List<String> harmonizationDatasetsIds = harmonizationDatasetRepository.findByStudyTablesStudyId(study.getId()).stream().map(h -> h.getId()).collect(toList());
    List<String> studyDatasetIds = studyDatasetRepository.findByStudyTableStudyId(study.getId()).stream().map(h -> h.getId()).collect(toList());
    List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream().map(n -> n.getId()).collect(toList());

    if(!harmonizationDatasetsIds.isEmpty() || !studyDatasetIds.isEmpty() || !networkIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap() {{
        put("harmonizationDataset", harmonizationDatasetsIds);
        put("studyDataset", studyDatasetIds);
        put("network", networkIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

  @Nullable
  private String generateId(@NotNull Study study) {
    ensureAcronym(study);
    return getNextId(study.getAcronym());
  }

  @Nullable
  private String getNextId(LocalizedString suggested) {
    if(suggested == null) return null;
    String prefix = suggested.asString().toLowerCase();
    if(Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;
    try {
      getStudyStateInternal(next);
      for(int i = 1; i <= 1000; i++) {
        next = prefix + "-" + i;
        getStudyStateInternal(next);
      }
      return null;
    } catch(NoSuchStudyException e) {
      return next;
    }
  }

  @Nullable
  public Study unpublish(String id) {
    StudyState studyState = studyStateRepository.findOne(id);
    return studyState == null ? null : unpublish(studyState);
  }

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id") })
  @Nullable
  public Study unpublish(StudyState studyState) {
    log.info("Unpublish state since there are no Git repo for study: {}", studyState.getId());
    studyState.resetRevisionsAhead();
    studyState.setPublishedTag(null);
    studyState.setRevisionStatus(DRAFT);
    studyStateRepository.save(studyState);
    Study study = studyRepository.findOne(studyState.getId());
    if (study != null) eventBus.post(new StudyUnpublishedEvent(study));
    return study;
  }

  /**
   * If there are no git repo for input study, the publish state is no longer valid, unpublish the study
   * @param studyState
   */
  private void ensureStudyGitRepository(@NotNull StudyState studyState) {
    if (!gitService.hasGitRepository(studyState.getId())) {
      unpublish(studyState);
    }
  }

  /**
   * If there are no git repo for input study, the publish state is no longer valid, unpublish the study
   * @param studyState
   */
  private void ensureStudyGitRepositoryAndSave(@NotNull StudyState studyState) {
    if (!gitService.hasGitRepository(studyState.getId())) {
      Study study = unpublish(studyState);
      if (study != null) {
        log.info("Recuperated Study '{}' from repository is saved backed to Git repo.", studyState.getId());
        gitService.save(study);
      }
    }
  }

  private void ensureAcronym(@NotNull Study study) {
    if(study.getAcronym() == null || study.getAcronym().isEmpty()) {
      study.setAcronym(study.getName().asAcronym());
    }
  }
}
