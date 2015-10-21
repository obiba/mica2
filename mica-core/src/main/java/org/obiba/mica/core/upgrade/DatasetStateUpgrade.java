package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetStateUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(DatasetStateUpgrade.class);

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Override
  public String getDescription() {
    return "Refactored datasets published state.";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("0.9.2");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing datasets published state upgrade");

    studyDatasetRepository.findAll().forEach(dataset -> {
      studyDatasetService.save(dataset);

      if(dataset.isPublished()) {
        studyDatasetService.publish(dataset.getId(), true);
      }
    });

    harmonizationDatasetRepository.findAll().forEach(dataset -> {
      harmonizationDatasetService.save(dataset);

      if(dataset.isPublished()) {
        harmonizationDatasetService.publish(dataset.getId(), true);
      }
    });
  }
}
