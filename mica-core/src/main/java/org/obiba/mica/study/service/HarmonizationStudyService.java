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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.study.ConstraintException;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.domain.Population;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Validated
public class HarmonizationStudyService extends AbstractStudyService<HarmonizationStudyState, HarmonizationStudy> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationStudyService.class);

  private static final String SEPARATOR = ":";

  @Inject
  private HarmonizationStudyRepository harmonizationStudyRepository;

  @Inject
  private HarmonizationStudyStateRepository harmonizationStudyStateRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private MicaConfigService micaConfigService;

  @Override
  protected void saveInternal(final HarmonizationStudy study, String comment, boolean cascade) {
    log.info("Saving harmonization study: {}", study.getId());

    // checks if population and dce are still the same
    String studyId = study.getId();
    if (studyId != null) {
      List<String> list = populationsAffected(study, harmonizationStudyRepository.findOne(study.getId()));
      if (list != null && list.size() > 0) {
        checkPopulationMissingConstraints(studyId, list);
      }
    }

    if (study.getLogo() != null && study.getLogo().isJustUploaded()) {
      fileStoreService.save(study.getLogo().getId());
      study.getLogo().setJustUploaded(false);
    }

    ImmutableSet<String> invalidRoles = ImmutableSet
        .copyOf(Sets.difference(study.membershipRoles(), Sets.newHashSet(micaConfigService.getConfig().getRoles())));

    invalidRoles.forEach(study::removeRole);

    HarmonizationStudyState studyState = findEntityState(study, HarmonizationStudyState::new);

    if (!study.isNew()) ensureGitRepository(studyState);

    studyState.incrementRevisionsAhead();
    harmonizationStudyStateRepository.save(studyState);

    study.setLastModifiedDate(DateTime.now());

    harmonizationStudyRepository.save(study);

    gitService.save(study, comment);
    eventBus.post(new DraftStudyUpdatedEvent(study));
  }

  @Override
  public String getTypeName() {
    return "harmonization-study";
  }

  public Map<String, List<String>> getPotentialUnpublishingConflicts(HarmonizationStudy study) {
    List<HarmonizationDataset> datasets = findAllPublishedDatasetsByStudy(study.getId());
    if (!datasets.isEmpty()) {
      return new HashMap<String, List<String>>() {{
        put("harmonizationDataset", datasets.stream().map(HarmonizationDataset::getId).collect(Collectors.toList()));
      }};
    }

    return Maps.newHashMap();
  }

  /**
   * Get all published {@link StudyDataset}s of a given {@link Study}.
   *
   * @return List of {@link StudyDataset}s
   */
  public List<HarmonizationDataset> findAllPublishedDatasetsByStudy(@NotNull String studyId) {
    return harmonizationDatasetRepository
        .findByHarmonizationTableStudyId(studyId)
        .stream()
        .filter(ds -> harmonizationDatasetStateRepository.findOne(ds.getId()).isPublished())
        .collect(Collectors.toList());
  }

  /**
   * @param study
   * @param publishing
   * @return
   */
  public Map<String, List<String>> getPotentialConflicts(HarmonizationStudy study, boolean publishing) {
    if (study.getId() != null) {
      HarmonizationStudy oldStudy = publishing ? study : harmonizationStudyRepository.findOne(study.getId());
      if (oldStudy != null) {
        List<String> populationUIDs = publishing ? toListOfPopulationUids(study) : populationsAffected(study, oldStudy);

        if (populationUIDs != null) {
          List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream()
              .map(AbstractGitPersistable::getId).collect(toList());
          List<String> harmoDatasetIds = findHarmonizedDatasetDependencies(study.getId(), populationUIDs);

          if (!harmoDatasetIds.isEmpty() || !networkIds.isEmpty()) {
            return new HashMap<String, List<String>>() {{
              put("harmonizationDataset", harmoDatasetIds);
              put("network", networkIds);
            }};
          }
        }
      }
    }

    return null;
  }

  private List<String> populationsAffected(HarmonizationStudy study, HarmonizationStudy oldStudy) {
    if (oldStudy != null) {
      List<String> newPopIDs = study.getPopulations().stream().map(Population::getId).collect(toList());
      List<String> oldPopIDs = oldStudy.getPopulations().stream().map(Population::getId).collect(toList());

      boolean isChangeSignificant = newPopIDs.size() <= oldPopIDs.size() && !newPopIDs.containsAll(oldPopIDs);
      if (isChangeSignificant) {
        oldPopIDs.removeAll(newPopIDs);
        return oldPopIDs;
      } else return null;
    } else return null;
  }

  @Override
  public List<String> findAllIds() {
    return harmonizationStudyRepository.findAllExistingIds().stream().map(Study::getId).collect(toList());
  }

  @Override
  protected MongoRepository<HarmonizationStudy, String> getRepository() {
    return harmonizationStudyRepository;
  }

  @Override
  protected EntityStateRepository<HarmonizationStudyState> getEntityStateRepository() {
    return harmonizationStudyStateRepository;
  }

  @Override
  protected Class<HarmonizationStudy> getType() {
    return HarmonizationStudy.class;
  }


  private List<String> findHarmonizedDatasetDependencies(String studyId, List<String> concatenatedIds) {
    return concatenatedIds.stream()
        .map(populationId ->
            harmonizationDatasetRepository.findByHarmonizationTableStudyIdAndHarmonizationTablePopulationId(
                studyId, populationId)
        )
        .reduce(Lists.newArrayList(), StudyService::listAddAll).stream()
        .map(AbstractGitPersistable::getId).distinct().collect(toList());
  }

  private void checkPopulationMissingConstraints(String studyId, List<String> popIDs) {
    List<String> harmoDatasetIds = findHarmonizedDatasetDependencies(studyId, popIDs);

    if (!harmoDatasetIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
        put("harmonizationDataset", harmoDatasetIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

  /**
   * @param study
   */
  protected void checkStudyConstraints(HarmonizationStudy study) {
    List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream().map(n -> n.getId())
        .collect(toList());

    if (!networkIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
        put("network", networkIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

  private List<String> toListOfPopulationUids(HarmonizationStudy study) {
    return study.getPopulations().stream().map(Population::getId).collect(toList());
  }

}
