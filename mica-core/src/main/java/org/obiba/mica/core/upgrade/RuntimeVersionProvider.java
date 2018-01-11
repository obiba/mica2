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

import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.VersionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RuntimeVersionProvider implements VersionProvider {

  private Version version;

  @Autowired
  public RuntimeVersionProvider(@Value("${version}") String version) {
    this.version = new Version(version);
  }

  @Override
  public Version getVersion() {
    return version;
  }
}
