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

import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.VersionModifier;
import org.springframework.stereotype.Component;

@Component
public class MicaVersionModifier implements VersionModifier {

  @Inject
  private MicaConfigService micaConfigService;

  @Override
  public Version getVersion() {
    return micaConfigService.getConfig().getMicaVersion();
  }

  @Override
  public void setVersion(Version version) {
    MicaConfig config = micaConfigService.getConfig();
    config.setMicaVersion(version);

    micaConfigService.save(config);
  }
}
