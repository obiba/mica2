package org.obiba.mica.study.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.git.CommitInfo;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileService;
import org.obiba.mica.file.service.FileSystemService;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import static java.util.stream.Collectors.toList;
import static org.obiba.mica.core.domain.RevisionStatus.DELETED;
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
  private AttachmentRepository attachmentRepository;

  @Inject
  private GitService gitService;

  @Inject
  private FileService fileService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private PersonRepository personRepository;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private EventBus eventBus;

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid Study study) {
    saveInternal(study, null);
  }

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid Study study, @Nullable String comment) {
    saveInternal(study, comment);
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

  private void saveInternal(Study study, String comment) {
    log.info("Saving study: {}", study.getId());
    StudyState studyState = findStudyState(study);

    if (!study.isNew()) ensureStudyGitRepository(studyState);

    if (study.getLogo() != null && study.getLogo().isJustUploaded()) {
      fileService.save(study.getLogo().getId());
      study.getLogo().setJustUploaded(false);
    }

    study.setContacts(replaceExistingPersons(study.getContacts()));
    study.setInvestigators(replaceExistingPersons(study.getInvestigators()));

    studyState.setName(study.getName());
    studyState.incrementRevisionsAhead();
    studyStateRepository.save(studyState);
    study.setLastModifiedDate(DateTime.now());
    studyRepository.saveWithReferences(study);
    gitService.save(study, comment);

    eventBus.post(new DraftStudyUpdatedEvent(study));
    study.getAllPersons().forEach(c -> eventBus.post(new PersonUpdatedEvent(c.getPerson())));
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

  public boolean isPublished(@NotNull String id) throws NoSuchStudyException {
    return findStateById(id).isPublished();
  }

  public List<StudyState> findAllStates() {
    return studyStateRepository.findAll();
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
    studyState.setPublishedTag(gitService.tag(studyState));
    studyState.resetRevisionsAhead();
    studyStateRepository.save(studyState);
    eventBus.post(new StudyPublishedEvent(studyRepository.findOne(id)));
    return studyState;
  }

  @Nullable
  public Study unPublish(String id) {
    StudyState studyState = studyStateRepository.findOne(id);
    return studyState == null ? null : unpublish(studyState);
  }


  public StudyState updateStatus(String id, RevisionStatus status) {
    StudyState studyState = findStateById(id);
    studyState.setRevisionStatus(status);
    studyStateRepository.save(studyState);
    return studyState;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
    log.info("Gather published and draft studies to be indexed");
    List<Study> publishedStudies = findPublishedStates().stream() //
      .filter(studyState -> { //
        return gitService.hasGitRepository(studyState) && !Strings.isNullOrEmpty(studyState.getPublishedTag()); //
      }) //
      .map(studyState -> gitService.readFromTag(studyState, studyState.getPublishedTag(), Study.class)) //
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

    gitService.deleteGitRepository(study);
    eventBus.post(new StudyDeletedEvent(study));
    studyStateRepository.delete(id);
    studyRepository.deleteWithReferences(study);
  }

  //
  // Private methods
  //

  private List<Person> replaceExistingPersons(List<Person> persons) {
    ImmutableList.copyOf(persons).forEach(c -> {
      if(c.getId() == null && c.getEmail() != null) {
        Person person = personRepository.findOneByEmail(c.getEmail());

        if(person != null) {
          int idx = persons.indexOf(c);
          persons.remove(c);
          persons.add(idx, person);
        }
      }
    });

    return persons;
  }

  private void checkStudyConstraints(Study study) {
    List<String> harmonizationDatasetsIds = harmonizationDatasetRepository.findByStudyTablesStudyId(study.getId()).stream().map(h -> h.getId()).collect(toList());
    List<String> studyDatasetIds = studyDatasetRepository.findByStudyTableStudyId(study.getId()).stream().map(
      h -> h.getId()).collect(toList());
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

  @Caching(evict = { @CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id") })
  @Nullable
  public Study unpublish(StudyState studyState) {
    log.info("Unpublish state since there are no Git repo for study: {}", studyState.getId());
    studyState.resetRevisionsAhead();
    studyState.setPublishedTag(null);
    if (studyState.getRevisionStatus() != DELETED) studyState.setRevisionStatus(DRAFT);
    studyStateRepository.save(studyState);
    Study study = studyRepository.findOne(studyState.getId());
    if (study != null) eventBus.post(new StudyUnpublishedEvent(study));
    return study;
  }

  /**
   * If there are no git repo for input study, the publish state is no longer valid, unpublish the study
   */
  private void ensureStudyGitRepository(@NotNull StudyState studyState) {
    if (!gitService.hasGitRepository(studyState)) {
      unpublish(studyState);
    }
  }

  /**
   * If there are no git repo for input study, the publish state is no longer valid, unpublish the study
   */
  private void ensureStudyGitRepositoryAndSave(@NotNull StudyState studyState) {
    if (!gitService.hasGitRepository(studyState)) {
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

  public Iterable<CommitInfo> getCommitInfos(@NotNull Study study) {
    return gitService.getCommitsInfo(study, Study.class);
  }

  public CommitInfo getCommitInfo(@NotNull Study study, @NotNull String commitInfo) {
    return gitService.getCommitInfo(study, commitInfo, Study.class);
  }

  public Study getStudyFromCommit(@NotNull Study study, @NotNull String commitId) throws IOException {
    String studyBlob = gitService.getBlob(study, commitId, Study.class);
    InputStream inputStream = new ByteArrayInputStream(studyBlob.getBytes(StandardCharsets.UTF_8));
    Study restoredStudy = objectMapper.readValue(inputStream, Study.class);

    Stream.concat(restoredStudy.getAttachments().stream(),
      restoredStudy.getPopulations().stream().flatMap(p -> p.getDataCollectionEvents().stream())
        .flatMap(d -> d.getAttachments().stream())).forEach(a -> {
      try {
        fileSystemService.getAttachmentState(a.getPath(), a.getName(), false);
      } catch(NoSuchEntityException e) {
        Attachment existingAttachment = attachmentRepository.findOne(a.getId());

        if(existingAttachment != null) {
          existingAttachment.setPath(existingAttachment.getPath().replaceAll("/attachment/[0-9a-f\\-]+$", ""));
          fileSystemService.save(existingAttachment);
        } else fileSystemService.save(a);
      }
    });

    return restoredStudy;
  }

  public Iterable<String> getDiffEntries(@NotNull Study study, @NotNull String commitId, @Nullable String prevCommitId) {
    return gitService.getDiffEntries(study, commitId, prevCommitId, Study.class);
  }

}
