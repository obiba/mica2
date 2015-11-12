package org.obiba.mica.core.upgrade;

import javax.inject.Inject;

import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NetworkStateUpgrade implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(NetworkStateUpgrade.class);

  @Inject
  private NetworkRepository networkRepository;

  @Inject
  private NetworkService networkService;

  @Override
  public String getDescription() {
    return "Refactored network published state.";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("1.0.0");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing networks published state upgrade");

    networkRepository.findAll().forEach(network -> {
      networkService.save(network);

      if(network.isPublished()) {
        networkService.publish(network.getId(), true);
      }
    });
  }
}
