package org.obiba.mica.core.upgrade;

import org.obiba.mica.core.domain.DefaultEntityBase;
import org.obiba.mica.study.service.CollectionStudyService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class Mica3Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica3Upgrade.class);

  @Inject
  private CollectionStudyService collectionStudyService;

  @Override
  public String getDescription() {
    return "Mica 3.0.0 upgrade";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 0, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.0.0");

    try {
      republishStudies();
    } catch (Exception e) {
      logger.error("Error occurred when republishing studies");
    }
  }

  private void republishStudies() {
    collectionStudyService.findPublishedStates().stream()
      .filter(s -> s.getRevisionsAhead() == 0)
      .map(DefaultEntityBase::getId)
      .forEach(s -> collectionStudyService.publish(s, true));
  }
}
