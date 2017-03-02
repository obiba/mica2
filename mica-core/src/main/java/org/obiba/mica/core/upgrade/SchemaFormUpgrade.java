/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.springframework.stereotype.Component;

@Component
public class SchemaFormUpgrade implements UpgradeStep {

  @Override
  public String getDescription() {
    return "Default study schema form has changed. " +
      "So we need to update model of studies if the new schema form is used (so if the application upgraded from 1.x.x to 2.1.x)";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(2, 1, 0);
  }

  @Override
  public boolean mustBeApplied(Version previousVersion, Version runtimeVersion) {
    return new Version(2, 0, 0).compareTo(previousVersion) > 0
      && new Version(2, 1, 0).compareTo(runtimeVersion) <= 0;
  }

  @Override
  public void execute(Version currentVersion) {

    //TO migration script

  }
}
