/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.service;

import java.util.NoSuchElementException;

public class NoSuchProjectException extends NoSuchElementException {

  private static final long serialVersionUID = 83912682720233942L;

  private NoSuchProjectException(String s) {
    super(s);
  }

  public static NoSuchProjectException withId(String id) {
    return new NoSuchProjectException("Project with id '" + id + "' does not exist");
  }

}
