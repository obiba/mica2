package org.obiba.mica.core.upgrade;

import org.obiba.mica.network.service.NetworkService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class NetworkSchemaformMigration implements UpgradeStep {

  @Inject
  private NetworkService networkService;

  private static final Logger log = LoggerFactory.getLogger(NetworkSchemaformMigration.class);

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
    log.debug("Indexing all networks in the repository.");
    networkService.indexAll();
  }
}
