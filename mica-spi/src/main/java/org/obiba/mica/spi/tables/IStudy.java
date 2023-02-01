/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.tables;

import org.obiba.mica.spi.search.Identified;

import java.util.Map;

/**
 * Study as it is exposed to plugins.
 */
public interface IStudy extends Identified {

  /**
   * Get the Opal URL, if any is defined specifically for this study.
   *
   * @return
   */
  String getOpal();

  /**
   * Get the study model.
   *
   * @return
   */
  Map<String, Object> getModel();
}
