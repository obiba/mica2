/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.file.service.DraftFileService;
import org.obiba.mica.file.service.PublishedFileService;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.NetworkStateRepository;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.project.ProjectRepository;
import org.obiba.mica.project.ProjectStateRepository;
import org.obiba.mica.project.service.PublishedProjectService;
import org.obiba.mica.study.EntityStateRepositoryCustom;
import org.obiba.mica.study.HarmonizationStudyStateRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class MicaMetricsService {

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private HarmonizationStudyStateRepository harmonizationStudyStateRepository;

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private NetworkStateRepository networkStateRepository;

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private StudyDatasetStateRepository studyDatasetStateRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Inject
  private PublishedDatasetVariableService publishedDatasetVariableService;

  @Inject
  private ProjectRepository projectRepository;

  @Inject
  private PublishedProjectService publishedProjectService;

  @Inject
  private ProjectStateRepository projectStateRepository;

  @Inject
  private DraftFileService draftFileService;

  @Inject
  private PublishedFileService publishedFileService;

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  private LinkedHashMap<String, Object> getStateCount(EntityStateRepositoryCustom repository) {
    List<LinkedHashMap> linkedHashMaps = repository.countByEachStateStatus(true);
    LinkedHashMap linkedHashMap = linkedHashMaps.get(0);
    linkedHashMap.remove("_id");
    return linkedHashMap;
  }

  // Individual Study
  public LinkedHashMap<String, Object> getIndividualStudiesStateCount() {
    return getStateCount(studyStateRepository);
  }

  public long getDraftIndividualStudiesCount() {
    return studyRepository.count();
  }

  public long getPublishedIndividualStudiesCount() {
    return publishedStudyService.getIndividualStudyCount();
  }

  public long getEditingIndividualStudiesCount() {
    return studyStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  public long getDraftIndividualStudiesFilesCount() {
    return draftFileService.getCount(Study.RESOURCE_PATH.toLowerCase());
  }

  public long getPublishedIndividualStudiesFilesCount() {
    return publishedFileService.getCount(Study.RESOURCE_PATH.toLowerCase());
  }

  public long getPublishedIndividualStudiesWithVariablesCount() {
    return publishedDatasetService.getStudiesWithVariablesCount();
  }

  public long getPublishedIndividualStudiesVariablesCount() {
    return publishedDatasetVariableService.getCountByVariableType(DatasetVariable.Type.Collected);
  }

  // Harmonization Study

  public LinkedHashMap<String, Object> getHarmonizationStudiesStateCount() {
    return getStateCount(harmonizationStudyStateRepository);
  }

  public long getDraftHarmonizationStudiesCount() {
    return harmonizationStudyStateRepository.count();
  }

  public long getPublishedHarmonizationStudiesCount() {
    return publishedStudyService.getHarmonizationStudyCount();
  }

  public long getEditingHarmonizationStudiesCount() {
    return harmonizationStudyStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  public long getDraftHarmonizationStudiesFilesCount() {
    return draftFileService.getCount(HarmonizationStudy.RESOURCE_PATH.toLowerCase());
  }

  public long getPublishedHarmonizationStudiesFilesCount() {
    return publishedFileService.getCount(HarmonizationStudy.RESOURCE_PATH.toLowerCase());
  }

  public long getPublishedHarmonizationStudiesVariablesCount() {
    return publishedDatasetVariableService.getCountByVariableType(DatasetVariable.Type.Dataschema);
  }

  // Network

  public LinkedHashMap<String, Object> getNetworksStateCount() {
    return getStateCount(networkStateRepository);
  }

  public long getDraftNetworksCount() {
    return networkRepository.count();
  }

  public long getPublishedNetworksCount() {
    return publishedNetworkService.getCount();
  }

  public long getEditingNetworksCount() {
    return networkStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  public long getDraftNetworkFilesCount() {
    return draftFileService.getCount(Network.class.getSimpleName().toLowerCase());
  }

  public long getPublishedNetworkFilesCount() {
    return publishedFileService.getCount(Network.class.getSimpleName().toLowerCase());
  }

  // Study Dataset

  public LinkedHashMap<String, Object> getStudyDatasetsStateCount() {
    return getStateCount(studyDatasetStateRepository);
  }

  public long getDraftStudyDatasetsCount() {
    return studyDatasetRepository.count();
  }

  public long getPublishedStudyDatasetsCount() {
    return publishedDatasetService.getStudyDatasetsCount();
  }

  public long getDraftStudyDatasetFilesCount() {
    return draftFileService.getCount("collected-dataset");
  }

  public long getPublishedStudyDatasetFilesCount() {
    return publishedFileService.getCount("collected-dataset");
  }

  // Harmonization Dataset

  public LinkedHashMap<String, Object> getHarmonizationDatasetsStateCount() {
    return getStateCount(harmonizationDatasetStateRepository);
  }


  public long getPublishedHarmonizationDatasetsCount() {
    return publishedDatasetService.getHarmonizationDatasetsCount();
  }

  public long getDraftHarmonizationDatasetFilesCount() {
    return draftFileService.getCount("harmonized-dataset");
  }

  public long getPublishedHarmonizationDatasetFilesCount() {
    return publishedFileService.getCount("harmonized-dataset");
  }

  // Project

  public LinkedHashMap<String, Object> getProjectsStateCount() {
    return getStateCount(projectStateRepository);
  }

  public long getPublishedProjectsCount() {
    return publishedProjectService.getCount();
  }

  public long getDraftProjectFilesCount() {
    return draftFileService.getCount("project");
  }

  public long getPublishedProjectFilesCount() {
    return publishedFileService.getCount("project");
  }

  // Data Access

  public LinkedHashMap<String, Object> getDataAccessRequestsStateCount() {
    return dataAccessRequestRepository.getCountByStatus().get(0);
  }

  // Variables
  public long getPublishedVariablesCount() {
    return publishedDatasetVariableService.getCount();
  }

  public long getHarmonizedVariablesCount() {
    return publishedDatasetVariableService.getHarmonizedCount();
  }
}
