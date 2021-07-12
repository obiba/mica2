/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.core.service.MissingCommentException;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.study.ConstraintException;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyPopulationDceWeightChangedEvent;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class IndividualStudyService extends AbstractStudyService<StudyState, Study> {

  private static final Logger log = LoggerFactory.getLogger(IndividualStudyService.class);

  private static final String SEPARATOR = ":";

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private MicaConfigService micaConfigService;

  @Override
  protected void saveInternal(final Study study, String comment, boolean cascade) {
    saveInternal(study, comment, cascade, false);
  }

  public void saveInternal(final Study study, String comment, boolean cascade, boolean weightChanged) {
    if (!Strings.isNullOrEmpty(study.getId()) && micaConfigService.getConfig().isCommentsRequiredOnDocumentSave() && Strings.isNullOrEmpty(comment)) {
      throw new MissingCommentException("Due to the server configuration, comments are required when saving this document.");
    }

    log.info("Saving study: {}", study.getId());

    // checks if population and dce are still the same
    if (study.getId() != null) {
      List<String> list = populationsOrDceAffected(study, studyRepository.findOne(study.getId()), false);
      if (list != null && list.size() > 0) {
        checkPopulationOrDceMissingConstraints(list);
      }
    }

    if (study.getLogo() != null && study.getLogo().isJustUploaded()) {
      fileStoreService.save(study.getLogo().getId());
      study.getLogo().setJustUploaded(false);
    }

    StudyState studyState = findEntityState(study, StudyState::new);

    if (!study.isNew()) ensureGitRepository(studyState);

    studyState.incrementRevisionsAhead();

    if (weightChanged) {
      studyState.setPopulationOrDceWeightChange(true);
    }

    studyStateRepository.save(studyState);
    study.setLastModifiedDate(DateTime.now());

    studyRepository.save(study);

    gitService.save(study, comment);
    eventBus.post(new DraftStudyUpdatedEvent(study));
  }

  public void save(Study study, String comment, boolean weightChanged) {
    saveInternal(study, comment, true, weightChanged);
  }

  @Override
  public String getTypeName() {
    return "individual-study";
  }

  @Override
  public Study getFromCommit(@NotNull Study study, @NotNull String commitId) {
    String studyBlob = gitService.getBlob(study, commitId, Study.class);
    InputStream inputStream = new ByteArrayInputStream(studyBlob.getBytes(StandardCharsets.UTF_8));
    Study restoredStudy;

    try {
      restoredStudy = objectMapper.readValue(inputStream, Study.class);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    return restoredStudy;
  }

  public JSONObject getFromCommitAsJson(@NotNull Study study, @NotNull String commitId) {
    String studyBlob = gitService.getBlob(study, commitId, Study.class);
    try {
      return new JSONObject(studyBlob);
    } catch (JSONException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected StudyState publishStateInternal(@NotNull String id) {
    StudyState studyState = super.publishStateInternal(id);
    if (studyState.isPopulationOrDceWeightChange()) {
      studyState.setPopulationOrDceWeightChange(false);
      eventBus.post(new DraftStudyPopulationDceWeightChangedEvent(findStudy(studyState.getId())));
    }

    return studyState;
  }

  @Override
  protected StudyState unPublishStateInternal(@NotNull String id) {
    StudyState studyState = super.unPublishStateInternal(id);
    // reaching here implies that none of the dependent documents are published
    studyState.setPopulationOrDceWeightChange(false);
    return studyState;
  }

  public Map<String, List<String>> getPotentialUnpublishingConflicts(Study study) {
    List<StudyDataset> datasets = findAllPublishedDatasetsByStudy(study.getId());
    if (!datasets.isEmpty()) {
      return new HashMap<String, List<String>>() {{
        put("studyDataset", datasets.stream().map(StudyDataset::getId).collect(Collectors.toList()));
      }};
    }

    return Maps.newHashMap();
  }

  /**
   * Get all published {@link StudyDataset}s of a given {@link Study}.
   *
   * @return List of {@link StudyDataset}s
   */
  public List<StudyDataset> findAllPublishedDatasetsByStudy(@NotNull String studyId) {
    return studyDatasetRepository
        .findByStudyTableStudyId(studyId)
        .stream()
        .filter(ds -> studyDatasetStateRepository.findOne(ds.getId()).isPublished())
        .collect(Collectors.toList());
  }

  public Map<String, List<String>> getPotentialConflicts(Study study, boolean publishing) {
    if (study.getId() != null) {
      Study oldStudy = publishing ? study : studyRepository.findOne(study.getId());
      if (oldStudy != null) {
        List<String> dceUIDs = publishing ? toListOfDceUids(study, true) : populationsOrDceAffected(study, oldStudy, true);

        if (dceUIDs != null) {
          List<String> studyDatasetIds = findStudyDatasetDependencies(dceUIDs);
          List<String> harmoDatasetIds = findHarmonizedDatasetDependencies(dceUIDs);
          List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream()
              .map(AbstractGitPersistable::getId).collect(toList());

          if (!harmoDatasetIds.isEmpty() || !studyDatasetIds.isEmpty() || !networkIds.isEmpty()) {
            return new HashMap<String, List<String>>() {{
              put("harmonizationDataset", harmoDatasetIds);
              put("studyDataset", studyDatasetIds);
              put("network", networkIds);
            }};
          }
        }
      }
    }

    return null;
  }

  //
  // Private methods
  //
  private List<String> populationsOrDceAffected(Study study, Study oldStudy, boolean withDceStartField) {
    if (oldStudy != null) {
      List<String> newDceUIDs = toListOfDceUids(study, withDceStartField);
      List<String> oldDceUIDs = toListOfDceUids(oldStudy, withDceStartField);

      boolean isChangeSignificant = newDceUIDs.size() <= oldDceUIDs.size() && !newDceUIDs.containsAll(oldDceUIDs);
      if (isChangeSignificant) {
        oldDceUIDs.removeAll(newDceUIDs);
        return oldDceUIDs;
      } else return null;
    } else return null;
  }

  private void checkPopulationOrDceMissingConstraints(List<String> dceUIDs) {
    List<String> studyDatasetIds = findStudyDatasetDependencies(dceUIDs);
    List<String> harmoDatasetIds = findHarmonizedDatasetDependencies(dceUIDs);

    if (!harmoDatasetIds.isEmpty() || !studyDatasetIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
        put("harmonizationDataset", harmoDatasetIds);
        put("studyDataset", studyDatasetIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

  protected void checkStudyConstraints(Study study) {
    List<String> harmonizationDatasetsIds = harmonizationDatasetRepository.findByStudyTablesStudyId(study.getId())
        .stream().map(h -> h.getId()).collect(toList());
    List<String> studyDatasetIds = studyDatasetRepository.findByStudyTableStudyId(study.getId()).stream()
        .map(AbstractGitPersistable::getId).collect(toList());
    List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream().map(n -> n.getId())
        .collect(toList());

    if (!harmonizationDatasetsIds.isEmpty() || !studyDatasetIds.isEmpty() || !networkIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
        put("harmonizationDataset", harmonizationDatasetsIds);
        put("studyDataset", studyDatasetIds);
        put("network", networkIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

  @Override
  public List<String> findAllIds() {
    return studyRepository.findAllExistingIds().stream().map(Study::getId).collect(Collectors.toList());
  }

  @Override
  protected MongoRepository<Study, String> getRepository() {
    return studyRepository;
  }

  @Override
  protected EntityStateRepository<StudyState> getEntityStateRepository() {
    return studyStateRepository;
  }

  @Override
  protected Class<Study> getType() {
    return Study.class;
  }

  // from dataCollectionEventUIDs
  private List<String> findStudyDatasetDependencies(List<String> concatenatedIds) {
    return concatenatedIds.stream()
        .map(o -> {
          String[] split = o.split(SEPARATOR);
          return studyDatasetRepository.findByStudyTableStudyIdAndStudyTablePopulationIdAndStudyTableDataCollectionEventId(
              split[0], split[1], split[2]);
        })
        .reduce(Lists.newArrayList(), StudyService::listAddAll).stream()
        .map(AbstractGitPersistable::getId).distinct().collect(toList());
  }

  // from dataCollectionEventUIDs
  private List<String> findHarmonizedDatasetDependencies(List<String> concatenatedIds) {
    return concatenatedIds.stream()
        .map(o -> {
          String[] split = o.split(SEPARATOR);
          return harmonizationDatasetRepository.findByStudyTablesStudyIdAndStudyTablesPopulationIdAndStudyTablesDataCollectionEventId(
              split[0], split[1], split[2]);
        })
        .reduce(Lists.newArrayList(), StudyService::listAddAll).stream()
        .map(AbstractGitPersistable::getId).distinct().collect(toList());
  }

  private List<String> toListOfDceUids(Study study, boolean withDceStartField) {
    return study.getPopulations().stream()
        .map(p -> p.getDataCollectionEvents().stream()
            .map(dce -> study.getId() + SEPARATOR + p.getId() + SEPARATOR + dce.getId() + (withDceStartField ? SEPARATOR + dce.getStart().getSortableYearMonth() : ""))
            .collect(toList()))
        .reduce(Lists.newArrayList(), StudyService::listAddAll);
  }
}
