/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

public class NoSuchCommentException extends RuntimeException{

  private static final long serialVersionUID = -3567676595158511379L;

  public NoSuchCommentException(String message) {
    super(message);
  }

  public static NoSuchCommentException withId(String id) {
    return new NoSuchCommentException("No Comment with id '" + id + "' exists.");
  }

}
