package org.obiba.mica.core.upgrade;

import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.StudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class Mica222PrepareMica3Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica222PrepareMica3Upgrade.class);

  @Inject
  private StudyService studyService;

  @Inject
  private NetworkService networkService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Override
  public String getDescription() {
    return "Mica 2.2.2 upgrade";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 2, 2);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 2.2.2. This upgrade is needed before upgrading to version 3");

    try {
      republishStudies();
    } catch (RuntimeException e) {
      logger.error("Error occurred when republishing studies");
    }

    try {
      republishNetworks();
    } catch (RuntimeException e) {
      logger.error("Error occurred when republishing networks");
    }

    try {
      republishHarmonizationDatasets();
    } catch (RuntimeException e) {
      logger.error("Error occurred when republishing harmonizationDatasets");
    }

    try {
      republishStudyDatasets();
    } catch (RuntimeException e) {
      logger.error("Error occurred when republishing studyDatasets");
    }
  }

  private void republishStudies() {

    List<Study> publishedStudies = studyService.findAllPublishedStudies();

    for (Study publishedStudy : publishedStudies) {
      StudyState studyState = studyService.getEntityState(publishedStudy.getId());
      if (studyState.getRevisionsAhead() == 0) {
        studyService.save(publishedStudy);
        studyService.publish(publishedStudy.getId(), true);
      } else {
        Study draftStudy = studyService.findStudy(publishedStudy.getId());
        studyService.save(publishedStudy);
        studyService.publish(publishedStudy.getId(), true);
        studyService.save(draftStudy);
      }
    }
  }

  private void republishNetworks() {
    List<Network> publishedNetworks = networkService.findAllPublishedNetworks();

    for (Network publishedNetwork : publishedNetworks) {
      NetworkState networkState = networkService.getEntityState(publishedNetwork.getId());
      if (networkState.getRevisionsAhead() == 0) {
        networkService.save(publishedNetwork);
        networkService.publish(publishedNetwork.getId(), true);
      } else {
        Network draftNetwork = networkService.findById(publishedNetwork.getId());
        networkService.save(publishedNetwork);
        networkService.publish(publishedNetwork.getId(), true);
        networkService.save(draftNetwork);
      }
    }
  }

  private void republishHarmonizationDatasets() {

    List<HarmonizationDataset> publishedHarmonizationDatasets = harmonizationDatasetService.findAllPublishedDatasets();

    for (HarmonizationDataset publishedHarmonizationDataset : publishedHarmonizationDatasets) {
      HarmonizationDatasetState harmonizationDatasetState = harmonizationDatasetService.getEntityState(publishedHarmonizationDataset.getId());
      if (harmonizationDatasetState.getRevisionsAhead() == 0) {
        harmonizationDatasetService.save(publishedHarmonizationDataset);
        harmonizationDatasetService.publish(publishedHarmonizationDataset.getId(), true);
      } else {
        HarmonizationDataset draftHarmonizationDataset = harmonizationDatasetService.findById(publishedHarmonizationDataset.getId());
        harmonizationDatasetService.save(publishedHarmonizationDataset);
        harmonizationDatasetService.publish(publishedHarmonizationDataset.getId(), true);
        harmonizationDatasetService.save(draftHarmonizationDataset);
      }
    }
  }

  private void republishStudyDatasets() {
    List<StudyDataset> publishedStudyDatasets = studyDatasetService.findAllPublishedDatasets();

    for (StudyDataset publishedStudyDataset : publishedStudyDatasets) {
      NetworkState studyDatasetState = networkService.getEntityState(publishedStudyDataset.getId());
      if (studyDatasetState.getRevisionsAhead() == 0) {
        studyDatasetService.save(publishedStudyDataset);
        studyDatasetService.publish(publishedStudyDataset.getId(), true);
      } else {
        StudyDataset draftStudyDataset = studyDatasetService.findById(publishedStudyDataset.getId());
        studyDatasetService.save(publishedStudyDataset);
        studyDatasetService.publish(publishedStudyDataset.getId(), true);
        studyDatasetService.save(draftStudyDataset);
      }
    }
  }
}
