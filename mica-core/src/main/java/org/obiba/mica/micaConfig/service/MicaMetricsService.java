/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import javax.inject.Inject;

import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetStateRepository;
import org.obiba.mica.dataset.service.DraftCollectedDatasetService;
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
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Service;

@Service
public class MicaMetricsService {

  @Inject
  private StudyRepository studyRepository;

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
  private DraftCollectedDatasetService draftCollectedDatasetService;

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

  // Study
  public long getStudiesCount() {
    return studyRepository.count();
  }

  public long getPublishedStudiesCount() {
    return publishedStudyService.getCount();
  }

  public long getDraftStudyFilesCount() {
    return draftFileService.getCount(Study.class.getSimpleName().toLowerCase());
  }

  public long getEditingStudiesCount() {
    return studyStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  public long getPublishedStudyFilesCount() {
    return publishedFileService.getCount(Study.class.getSimpleName().toLowerCase());
  }

  public long getStudiesWithVariablesCount() {
    return draftCollectedDatasetService.getStudiesWithVariablesCount();
  }

  public long getPublishedStudiesWithVariablesCount() {
    return publishedDatasetService.getStudiesWithVariablesCount();
  }

  // Network
  public long getNetworksCount() {
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
  public long getStudyDatasetsCount() {
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

  public long getEditingStudyDatasetsCount() {
    return studyDatasetStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  // Harmonization Dataset
  public long getHarmonizarionDatasetsCount() {
    return harmonizationDatasetRepository.count();
  }

  public long getPublishedHarmonizationDatasetsCount() {
    return publishedDatasetService.getHarmonizationDatasetsCount();
  }

  public long getEditingHarmonizationDatasetsCount() {
    return harmonizationDatasetStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  public long getDraftHarmonizationDatasetFilesCount() {
    return draftFileService.getCount("harmonized-dataset");
  }

  public long getPublishedHarmonizationDatasetFilesCount() {
    return publishedFileService.getCount("harmonized-dataset");
  }

  // Project
  public long getProjectsCount() {
    return projectRepository.count();
  }

  public long getPublishedProjectsCount() {
    return publishedProjectService.getCount();
  }

  public long getEditingProjectsCount() {
    return projectStateRepository.countByPublishedTagNotNullAndRevisionsAheadGreaterThanEqual(1);
  }

  public long getProjectFilesCount() {
    return draftFileService.getCount("project");
  }

  public long getPublishedProjectFilesCount() {
    return publishedFileService.getCount("project");
  }

  // Variables
  public long getPublishedVariablesCount() {
    return publishedDatasetVariableService.getCount();
  }
}
