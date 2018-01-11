/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.obiba.plugins.PluginResources;

import java.io.File;

public class MicaPlugin extends PluginResources {

  public MicaPlugin(File directory) {
    super(directory);
  }

  @Override
  public String getHostVersionKey() {
    return "mica.version";
  }

  @Override
  public String getHostHome() {
    return System.getProperty("MICA_HOME");
  }
}
