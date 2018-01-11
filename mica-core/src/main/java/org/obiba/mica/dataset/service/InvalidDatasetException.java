/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.service;

public class InvalidDatasetException extends RuntimeException {

  public InvalidDatasetException() {
    super();
  }

  public InvalidDatasetException(String message) {
    super(message);
  }

  public InvalidDatasetException(Throwable e) {
    super(e);
  }

  public InvalidDatasetException(String message, Throwable e) {
    super(message, e);
  }
}
