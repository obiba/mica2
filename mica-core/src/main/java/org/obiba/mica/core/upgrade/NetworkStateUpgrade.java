/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
