package org.obiba.mica.core.upgrade;

import org.obiba.mica.study.service.StudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StudySchemaformMigration implements UpgradeStep {

  @Inject
  private StudyService studyService;

  private static final Logger log = LoggerFactory.getLogger(StudySchemaformMigration.class);

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
    log.debug("Indexing all studies in the repository.");
    studyService.indexAll();
  }
}
