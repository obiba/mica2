package org.obiba.mica.study.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.NoSuchEntityException;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.AbstractGitPersistableService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

@Service
@Validated
public class HarmonizationStudyService extends
  AbstractGitPersistableService<HarmonizationStudyState, HarmonizationStudy> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationStudyService.class);


  @Inject
  private HarmonizationStudyRepository studyRepository;

  @Inject
  private HarmonizationStudyStateRepository studyStateRepository;

  @Inject
  private EventBus eventBus;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private ModelAwareTranslator modelAwareTranslator;

  @Override
  protected EntityStateRepository<HarmonizationStudyState> getEntityStateRepository() {
    return studyStateRepository;
  }

  @Override
  protected Class<HarmonizationStudy> getType() {
    return HarmonizationStudy.class;
  }

  @Override
  public String getTypeName() {
    return "harmonizationStudy";
  }

  public List<HarmonizationStudy> findAllDraftStudies() {
    return studyRepository.findAll();
  }

  public List<HarmonizationStudy> findAllDraftStudies(Iterable<String> ids) {
    return Lists.newArrayList(studyRepository.findAll(ids));
  }

  @Override
  public HarmonizationStudy findDraft(String id) throws NoSuchEntityException {
    return findDraft(id, null);
  }

  @NotNull
  public HarmonizationStudy findDraft(@NotNull String id, String locale) throws NoSuchEntityException {

    // ensure study exists
    getEntityState(id);

    HarmonizationStudy study = studyRepository.findOne(id);

    if (locale != null) {
      modelAwareTranslator.translateModel(locale, study);
    }

    return study;
  }

  @Override
  public void save(HarmonizationStudy study, String comments) {
    saveInternal(study, null, true);
  }


  @Caching(evict = {@CacheEvict(value = "aggregations-metadata", allEntries = true),
    @CacheEvict(value = {"studies-draft", "studies-published"}, key = "#id")})
  public void delete(@NotNull String id) {
    HarmonizationStudy study = studyRepository.findOne(id);

    if (study == null) {
      throw NoSuchEntityException.withId(HarmonizationStudy.class, id);
    }

//    checkStudyConstraints(study);

    fileSystemService.delete(FileUtils.getEntityPath(study));
    studyStateRepository.delete(id);
    studyRepository.deleteWithReferences(study);
    gitService.deleteGitRepository(study);
    eventBus.post(new StudyDeletedEvent(study));
  }

  @NotNull
  public HarmonizationStudy findStudy(@NotNull String id) throws NoSuchEntityException {
    // ensure study exists
    HarmonizationStudy study = studyRepository.findOne(id);
    if (study == null) throw NoSuchEntityException.withId(HarmonizationStudy.class, id);
    return study;
  }

  public boolean isPublished(@NotNull String id) throws NoSuchEntityException {
    return getEntityState(id).isPublished();
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
    HarmonizationStudy study = studyRepository.findOne(id);

    if (publish) {
      publishState(id);
      eventBus.post(new StudyPublishedEvent(study, getCurrentUsername(), cascadingScope));
    } else {
      unPublishState(id);
      eventBus.post(new StudyUnpublishedEvent(study));
    }
  }

  @Override
  public HarmonizationStudy getFromCommit(@NotNull HarmonizationStudy study, @NotNull String commitId) {
    String studyBlob = gitService.getBlob(study, commitId, Study.class);
    InputStream inputStream = new ByteArrayInputStream(studyBlob.getBytes(StandardCharsets.UTF_8));
    HarmonizationStudy restoredStudy;

    try {
      restoredStudy = objectMapper.readValue(inputStream, HarmonizationStudy.class);
    } catch(IOException e) {
      throw Throwables.propagate(e);
    }

//    restoredStudy.getAttachments().stream()
//      .forEach(a -> ensureAttachmentState(a, String.format("/harmonization-study/%s", restoredStudy.getId())));
//
//    restoredStudy.getPopulations().stream().forEach(p -> p.getDataCollectionEvents().stream().forEach(
//      d -> d.getAttachments().stream().forEach(a -> ensureAttachmentState(a, String
//        .format("/harmonization-study/%s/population/%s", restoredStudy.getId(), p.getId())))));

    return restoredStudy;
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
//
//  private List<String> populationsAffected(HarmonizationStudy study, HarmonizationStudy oldStudy) {
//    if (oldStudy != null) {
//      List<String> newPopIDs = study.getPopulations().stream().map(Population::getId).collect(toList());
//      List<String> oldPopIDs = oldStudy.getPopulations().stream().map(Population::getId).collect(toList());
//
//      boolean isChangeSignificant = newPopIDs.size() <= oldPopIDs.size() && !newPopIDs.containsAll(oldPopIDs);
//      if(isChangeSignificant) {
//        oldPopIDs.removeAll(newPopIDs);
//        return oldPopIDs;
//      } else return null;
//    } else return null;
//  }

//  private List<String> findHarmonizedDatasetDependencies(List<String> concatenatedIds) {
//    return concatenatedIds.stream()
//      .map(o -> {
//        String[] split = o.split(SEPARATOR);
//        return harmonizationDatasetRepository.findByStudyTablesStudyIdAndStudyTablesPopulationIdAndStudyTablesDataCollectionEventId(
//          split[0], split[1], split[2]);
//      })
//      .reduce(Lists.newArrayList(), this::listAddAll).stream()
//      .map(AbstractGitPersistable::getId).distinct().collect(toList());
//  }
//
//  private void checkPopulationMissingConstraints(List<String> popIDs) {
//    List<String> harmoDatasetIds = findHarmonizedDatasetDependencies(popIDs);
//
//    if (!harmoDatasetIds.isEmpty()) {
//      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
//        put("harmonizationDataset", harmoDatasetIds);
//      }};
//
//      throw new ConstraintException(conflicts);
//    }
//  }
//
//  private void checkStudyConstraints(Study study) {
//    List<String> harmonizationDatasetsIds = harmonizationDatasetRepository.findByStudyTablesStudyId(study.getId())
//      .stream().map(h -> h.getId()).collect(toList());
//    List<String> studyDatasetIds = studyDatasetRepository.findByStudyTableStudyId(study.getId()).stream()
//      .map(h -> h.getId()).collect(toList());
//    List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream().map(n -> n.getId())
//      .collect(toList());
//
//    if (!harmonizationDatasetsIds.isEmpty() || !studyDatasetIds.isEmpty() || !networkIds.isEmpty()) {
//      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
//        put("harmonizationDataset", harmonizationDatasetsIds);
//        put("studyDataset", studyDatasetIds);
//        put("network", networkIds);
//      }};
//
//      throw new ConstraintException(conflicts);
//    }
//  }

  private void saveInternal(final HarmonizationStudy study, String comment, boolean cascade) {
    log.info("Saving harmonization study: {}", study.getId());

    // checks if population and dce are still the same
//    if (study.getId() != null) {
//      List<String> list = populationsAffected(study, studyRepository.findOne(study.getId()));
//      if (list != null && list.size() > 0) {
//        checkPopulationMissingConstraints(list);
//      }
//    }

    if (study.getLogo() != null && study.getLogo().isJustUploaded()) {
      fileStoreService.save(study.getLogo().getId());
      study.getLogo().setJustUploaded(false);
    }

    ImmutableSet<String> invalidRoles = ImmutableSet
      .copyOf(Sets.difference(study.membershipRoles(), Sets.newHashSet(micaConfigService.getConfig().getRoles())));

    invalidRoles.forEach(r -> study.removeRole(r));

    HarmonizationStudyState studyState = findEntityState(study, () -> {
      HarmonizationStudyState defaultState = new HarmonizationStudyState();
//      defaultState.setName(study.getName());
      return defaultState;
    });

    if (!study.isNew()) ensureGitRepository(studyState);

//    studyState.setName(study.getName());
    studyState.incrementRevisionsAhead();
    studyStateRepository.save(studyState);

//    study.setName(studyState.getName());
    study.setLastModifiedDate(DateTime.now());

    if(cascade) studyRepository.saveWithReferences(study);
    else studyRepository.save(study);

    gitService.save(study, comment);
    eventBus.post(new DraftStudyUpdatedEvent(study));
  }


//  public Map<String, List<String>> getPotentialConflicts(HarmonizationStudy study, boolean publishing) {
//    if (study.getId() != null) {
//      HarmonizationStudy oldStudy = publishing ? study : studyRepository.findOne(study.getId());
//      if (oldStudy != null) {
//          List<String> harmoDatasetIds = findHarmonizedDatasetDependencies(dceUIDs);
//          List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream()
//            .map(AbstractGitPersistable::getId).collect(toList());
//
//          if (!harmoDatasetIds.isEmpty() || !studyDatasetIds.isEmpty() || !networkIds.isEmpty()) {
//            return new HashMap<String, List<String>>() {{
//              put("harmonizationDataset", harmoDatasetIds);
//              put("studyDataset", studyDatasetIds);
//              put("network", networkIds);
//            }};
//          }
//        }
//      }
//    }
//
//    return null;
//  }

  private void ensureAcronym(@NotNull HarmonizationStudy study) {
    if (study.getAcronym() == null || study.getAcronym().isEmpty()) {
      study.setAcronym(study.getName().asAcronym());
    }
  }

  @Nullable
  private String getNextId(LocalizedString suggested) {
    if (suggested == null) return null;
    String prefix = suggested.asUrlSafeString().toLowerCase();
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
  protected String generateId(HarmonizationStudy study) {
    ensureAcronym(study);
    return getNextId(study.getAcronym());
  }
}
