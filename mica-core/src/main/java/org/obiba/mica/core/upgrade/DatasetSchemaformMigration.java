package org.obiba.mica.core.upgrade;

import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DatasetSchemaformMigration implements UpgradeStep {

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  private static final Logger log = LoggerFactory.getLogger(DatasetSchemaformMigration.class);

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public Version getAppliesTo() {
    return new Version("2.0.0");
  }

  @Override
  public void execute(Version version) {
    log.debug("Indexing all study datasets in the repository.");
    studyDatasetService.indexAll();

    log.debug("Indexing all harmonization datasets in the repository.");
    harmonizationDatasetService.indexAll();
  }
}
