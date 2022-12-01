/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.source;

/**
 * Study table is to be extracted from a file which path applies to the Mica's internal file system.
 */
public interface StudyTableFileSource extends StudyTableStreamSource {

  /**
   * The path to the file in Mica's file system.
   *
   * @return
   */
  String getPath();

}
