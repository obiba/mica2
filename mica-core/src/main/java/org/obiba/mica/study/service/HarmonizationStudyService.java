/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.core.ModelAwareTranslator;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.service.FileSystemService;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@Service
@Validated
public class HarmonizationStudyService extends AbstractStudyService<HarmonizationStudyState, HarmonizationStudy> {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationStudyService.class);

  @Inject
  private HarmonizationStudyRepository studyRepository;

  @Inject
  private HarmonizationStudyStateRepository studyStateRepository;

  @Inject
  private NetworkRepository networkRepository;

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
  protected void saveInternal(final HarmonizationStudy study, String comment, boolean cascade) {
    log.info("Saving harmonization study: {}", study.getId());

    // checks if population and dce are still the same
    if(study.getId() != null) {
      List<String> list = populationsAffected(study, studyRepository.findOne(study.getId()));
      if(list != null && list.size() > 0) {
        log.info("TBI checkPopulationMissingConstraints()");
//        checkPopulationMissingConstraints(list);
      }
    }

    if(study.getLogo() != null && study.getLogo().isJustUploaded()) {
      fileStoreService.save(study.getLogo().getId());
      study.getLogo().setJustUploaded(false);
    }

    ImmutableSet<String> invalidRoles = ImmutableSet
      .copyOf(Sets.difference(study.membershipRoles(), Sets.newHashSet(micaConfigService.getConfig().getRoles())));

    invalidRoles.forEach(study::removeRole);

    HarmonizationStudyState studyState = findEntityState(study, HarmonizationStudyState::new);

    if(!study.isNew()) ensureGitRepository(studyState);

    studyState.incrementRevisionsAhead();
    studyStateRepository.save(studyState);

    study.setLastModifiedDate(DateTime.now());

    if(cascade) studyRepository.saveWithReferences(study);
    else studyRepository.save(study);

    gitService.save(study, comment);
    eventBus.post(new DraftStudyUpdatedEvent(study));
  }

  @Override
  public String getTypeName() {
    return "harmonization-study";
  }

  /**
   * TODO account for HarmonizationDataset constraint
   *
   * @param study
   * @param publishing
   * @return
   */
  public Map<String, List<String>> getPotentialConflicts(HarmonizationStudy study, boolean publishing) {
    if(study.getId() != null) {
      HarmonizationStudy oldStudy = publishing ? study : studyRepository.findOne(study.getId());
      if(oldStudy != null) {
        List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream()
          .map(AbstractGitPersistable::getId).collect(Collectors.toList());

        if(!networkIds.isEmpty()) {
          return new HashMap<String, List<String>>() {{
            put("network", networkIds);
          }};
        }
      }
    }

    return null;
  }

  private List<String> populationsAffected(HarmonizationStudy study, HarmonizationStudy oldStudy) {
    if(oldStudy != null) {
      List<String> newPopIDs = study.getPopulations().stream().map(Population::getId).collect(Collectors.toList());
      List<String> oldPopIDs = oldStudy.getPopulations().stream().map(Population::getId).collect(Collectors.toList());

      boolean isChangeSignificant = newPopIDs.size() <= oldPopIDs.size() && !newPopIDs.containsAll(oldPopIDs);
      if(isChangeSignificant) {
        oldPopIDs.removeAll(newPopIDs);
        return oldPopIDs;
      } else return null;
    } else return null;
  }

  @Override
  public List<String> findAllExistingIds(Iterable<String> ids) {
    return studyRepository.findAllExistingIds(ids).stream().map(Study::getId).collect(Collectors.toList());
  }

  @Override
  protected MongoRepository<HarmonizationStudy, String> getRepository() {
    return studyRepository;
  }

  @Override
  protected EntityStateRepository<HarmonizationStudyState> getEntityStateRepository() {
    return studyStateRepository;
  }

  @Override
  protected Class<HarmonizationStudy> getType() {
    return HarmonizationStudy.class;
  }


// TODO account for HarmonizationDataset constraint once the link between the HarmonizationStudy and HarmonizationDataset is made
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

  /**
   * TODO account for HarmonizationDataset constraint
   *
   * @param study
   */
  protected void checkStudyConstraints(HarmonizationStudy study) {
    List<String> networkIds = networkRepository.findByStudyIds(study.getId()).stream().map(n -> n.getId())
      .collect(Collectors.toList());

    if(!networkIds.isEmpty()) {
      Map<String, List<String>> conflicts = new HashMap<String, List<String>>() {{
        put("network", networkIds);
      }};

      throw new ConstraintException(conflicts);
    }
  }

}
