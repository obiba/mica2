package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Mica510upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica510upgrade.class);

  private CacheService cacheService;

  @Inject
  public Mica510upgrade(CacheService cacheService) {
    this.cacheService = cacheService;
  }

  @Override
  public String getDescription() {
    return "Clear caches for 5.1.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(5, 1, 0);
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("Executing Mica upgrade to version 5.1.0");
    cacheService.clearAllCaches();
  }
  
}
