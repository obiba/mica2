/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

public class FileRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 497002696064965475L;

  public FileRuntimeException(String filename) {
    this(filename, null);
  }

  public FileRuntimeException(String filename, Exception cause) {
    super(String.format("File not accessible: %s", filename), cause);
  }
}
