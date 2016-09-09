/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.obiba.git.NoSuchGitRepositoryException;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NetworkLogoMigration implements UpgradeStep {
  private static final Logger log = LoggerFactory.getLogger(NetworkLogoMigration.class);

  @Inject
  private GitService gitService;

  @Inject
  private FileStoreService fileStoreService;

  @Inject
  private NetworkService networkService;

  @Override
  public String getDescription() {
    return "Migrate Network logo from git to GridFS";
  }

  @Override
  public Version getAppliesTo() {
    return new Version("0.9");
  }

  @Override
  public void execute(Version version) {
    log.info("Executing network logo migration from git to GridFS");
    List<Network> networks = networkService.findAllNetworks();

    networks.forEach(network -> {
      try {
        AbstractGitPersistable persistable = new AbstractGitPersistable() {
          @Override
          public String pathPrefix() {
            return "networks";
          }

          @Override
          public Map<String, Serializable> parts() {
            return new HashMap<String, Serializable>() {
              {
                put(network.getClass().getSimpleName(), network);
              }
            };
          }
        };

        persistable.setId(network.getId());

        try {
          if(network.getLogo() != null) {
            byte[] ba = gitService.readFileHead(persistable, network.getLogo().getId());
            fileStoreService.save(network.getLogo().getId(), new ByteArrayInputStream(ba));
          }

          gitService.deleteGitRepository(persistable);
        } catch(RuntimeException e) {
          log.warn("Error in network upgrade. Ignoring.", e);
        }
      } catch(NoSuchGitRepositoryException ex) {
        //ignore: already migrated
      }
    });

    networkService.indexAll();
  }
}
