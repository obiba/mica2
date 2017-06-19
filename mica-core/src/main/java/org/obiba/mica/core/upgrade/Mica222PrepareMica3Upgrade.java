package org.obiba.mica.core.upgrade;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.study.service.StudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
    studyService.findPublishedStates().stream()
      .filter(study -> study.getRevisionsAhead() == 0)
      .map(AbstractGitPersistable::getId)
      .forEach(study -> studyService.publish(study, true));
  }

  private void republishNetworks() {
    networkService.findPublishedStates().stream()
      .filter(network -> network.getRevisionsAhead() == 0)
      .map(AbstractGitPersistable::getId)
      .forEach(network -> networkService.publish(network, true));
  }

  private void republishHarmonizationDatasets() {
    harmonizationDatasetService.findPublishedStates().stream()
      .filter(harmonizationDataset -> harmonizationDataset.getRevisionsAhead() == 0)
      .map(AbstractGitPersistable::getId)
      .forEach(harmonizationDataset -> harmonizationDatasetService.publish(harmonizationDataset, true));
  }

  private void republishStudyDatasets() {
    studyDatasetService.findPublishedStates().stream()
      .filter(studyDataset -> studyDataset.getRevisionsAhead() == 0)
      .map(AbstractGitPersistable::getId)
      .forEach(studyDataset -> studyDatasetService.publish(studyDataset, true));
  }
}
