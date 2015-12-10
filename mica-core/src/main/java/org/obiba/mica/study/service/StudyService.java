package org.obiba.mica.study.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.event.MicaConfigUpdatedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.study.ConstraintException;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class StudyService extends AbstractGitPersistableService<StudyState, Study> implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log = LoggerFactory.getLogger(StudyService.class);

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private FileStoreService fileStoreService;

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
  private EventBus eventBus;

  @Inject
  private MicaConfigService micaConfigService;

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid Study study) {
    saveInternal(study, null, true);
  }

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid Study study, @Nullable String comment) {
    saveInternal(study, comment, true);
  }

  private void saveInternal(final Study study, String comment, boolean cascade) {
    log.info("Saving study: {}", study.getId());

    if (study.getLogo() != null && study.getLogo().isJustUploaded()) {
      fileStoreService.save(study.getLogo().getId());
      study.getLogo().setJustUploaded(false);
    }

    study.setContacts(replaceExistingPersons(study.getContacts()));
    study.setInvestigators(replaceExistingPersons(study.getInvestigators()));

    ImmutableSet<String> invalidRoles = ImmutableSet
      .copyOf(Sets.difference(study.membershipRoles(), Sets.newHashSet(micaConfigService.getConfig().getRoles())));

    invalidRoles.forEach(r -> study.removeRole(r));

    StudyState studyState = findEntityState(study, () -> {
      StudyState defaultState = new StudyState();
      defaultState.setName(study.getName());
      return defaultState;
    });

    if (!study.isNew()) ensureGitRepository(studyState);

    studyState.setName(study.getName());
    studyState.incrementRevisionsAhead();
    studyStateRepository.save(studyState);

    study.setName(studyState.getName());
    study.setLastModifiedDate(DateTime.now());

    if(cascade) studyRepository.saveWithReferences(study);
    else studyRepository.save(study);

    gitService.save(study, comment);
    eventBus.post(new DraftStudyUpdatedEvent(study));
  }

  @NotNull
  @Cacheable(value = "studies-draft", key = "#id")
  public Study findDraft(@NotNull String id) throws NoSuchEntityException {
    // ensure study exists
    getEntityState(id);

    return studyRepository.findOne(id);
  }

  @NotNull
  public Study findStudy(@NotNull String id) throws NoSuchEntityException {
    // ensure study exists
    Study study = studyRepository.findOne(id);
    if (study == null) throw NoSuchEntityException.withId(Study.class, id);
    return study;
  }

  public boolean isPublished(@NotNull String id) throws NoSuchEntityException {
    return getEntityState(id).isPublished();
  }

  public List<Study> findAllDraftStudies() {
    return studyRepository.findAll();
  }

  public List<Study> findAllDraftStudies(Iterable<String> ids) {
    return Lists.newArrayList(studyRepository.findAll(ids));
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

  @Caching(evict = {@CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = {"studies-draft", "studies-published"}, key = "#id")})
  public void delete(@NotNull String id) {
    Study study = studyRepository.findOne(id);

    if (study == null) {
      throw NoSuchEntityException.withId(Study.class, id);
    }

    checkStudyConstraints(study);

    fileSystemService.delete(FileUtils.getEntityPath(study));
    studyStateRepository.delete(id);
    studyRepository.deleteWithReferences(study);
    gitService.deleteGitRepository(study);
    eventBus.post(new StudyDeletedEvent(study));
  }

  @Caching(evict = {@CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = {"studies-draft", "studies-published"}, key = "#id")})
  public void publish(@NotNull String id, boolean publish) throws NoSuchEntityException {
    publish(id, publish, PublishCascadingScope.NONE);
  }

  @Caching(evict = {@CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = {"studies-draft", "studies-published"}, key = "#id")})
  public void publish(@NotNull String id, boolean publish, PublishCascadingScope cascadingScope) throws NoSuchEntityException {
    log.info("Publish study: {}", id);
    Study study = studyRepository.findOne(id);

    if (publish) {
      publishState(id);
      eventBus.post(new StudyPublishedEvent(study, getCurrentUsername(), cascadingScope));
    } else {
      unPublishState(id);
      eventBus.post(new StudyUnpublishedEvent(study));
    }
  }

  @Override
  public String getTypeName() {
    return "study";
  }

  @Override
  public Study getFromCommit(@NotNull Study study, @NotNull String commitId) {
    String studyBlob = gitService.getBlob(study, commitId, Study.class);
    InputStream inputStream = new ByteArrayInputStream(studyBlob.getBytes(StandardCharsets.UTF_8));
    Study restoredStudy;

    try {
      restoredStudy = objectMapper.readValue(inputStream, Study.class);
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }

    restoredStudy.getAttachments().stream()
      .forEach(a -> ensureAttachmentState(a, String.format("/study/%s", restoredStudy.getId())));
    restoredStudy.getPopulations().stream().forEach(p -> p.getDataCollectionEvents().stream().forEach(
      d -> d.getAttachments().stream().forEach(a -> ensureAttachmentState(a, String
        .format("/study/%s/population/%s/data-collection-event/%s", restoredStudy.getId(), p.getId(), d.getId())))));

    return restoredStudy;
  }

  @Async
  @Subscribe
  public void micaConfigUpdated(MicaConfigUpdatedEvent event) {
    log.info("Mica config updated. Removing roles.");
    if(!event.getRemovedRoles().isEmpty())
      findAllDraftStudies().forEach(s -> removeRoles(s, event.getRemovedRoles()));
  }

  //
  // Private methods
  //

  private List<Person> replaceExistingPersons(List<Person> persons) {
    ImmutableList.copyOf(persons).forEach(c -> {
      if (c.getId() == null && !Strings.isNullOrEmpty(c.getEmail())) {
        Person person = personRepository.findOneByEmail(c.getEmail());

        if (person != null) {
          int idx = persons.indexOf(c);
          persons.remove(c);
          persons.add(idx, person);
        }
      }
    });

    return persons;
  }

  private void checkStudyConstraints(Study study) {
    List<String> harmonizationDatasetsIds = harmonizationDatasetRepository.findByStudyTablesStudyId(study.getId())
      .stream().map(h -> h.getId()).collect(toList());
    List<String> studyDatasetIds = studyDatasetRepository.findByStudyTableStudyId(study.getId()).stream()
      .map(h -> h.getId()).collect(toList());
    List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream().map(n -> n.getId())
      .collect(toList());

    if (!harmonizationDatasetsIds.isEmpty() || !studyDatasetIds.isEmpty() || !networkIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap() {{
        put("harmonizationDataset", harmonizationDatasetsIds);
        put("studyDataset", studyDatasetIds);
        put("network", networkIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

  @Nullable
  @Override
  protected String generateId(@NotNull Study study) {
    ensureAcronym(study);
    return getNextId(study.getAcronym());
  }

  @Nullable
  private String getNextId(LocalizedString suggested) {
    if (suggested == null) return null;
    String prefix = suggested.asString().toLowerCase();
    if (Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;
    try {
      getEntityState(next);
      for (int i = 1; i <= 1000; i++) {
        next = prefix + "-" + i;
        getEntityState(next);
      }
      return null;
    } catch (NoSuchEntityException e) {
      return next;
    }
  }

  @Override
  protected EntityStateRepository<StudyState> getEntityStateRepository() {
    return studyStateRepository;
  }

  @Override
  protected Class<Study> getType() {
    return Study.class;
  }

  private void removeRoles(@NotNull Study study, Iterable<String> roles) {
    saveInternal(study, String.format("Removed roles: %s", Joiner.on(", ").join(roles)), false);
    StudyState state = findStateById(study.getId());

    if(state.isPublished()) {
      publishState(study.getId());
      eventBus.post(new StudyPublishedEvent(study, getCurrentUsername(), PublishCascadingScope.NONE));
    }
  }

  private void ensureAttachmentState(Attachment a, String path) {
    if (!fileSystemService.hasAttachmentState(a.getPath(), a.getName(), false)) {
      Attachment existingAttachment = attachmentRepository.findOne(a.getId());

      if (existingAttachment != null) {
        if (!fileSystemService.hasAttachmentState(existingAttachment.getPath(), existingAttachment.getName(), false)) {
          existingAttachment.setPath(path);
          fileSystemService.reinstate(existingAttachment);
        }
      } else {
        log.warn("Missing attachment from git. Ignoring.", a);
      }
    }
  }

  private void ensureAcronym(@NotNull Study study) {
    if (study.getAcronym() == null || study.getAcronym().isEmpty()) {
      study.setAcronym(study.getName().asAcronym());
    }
  }
}
