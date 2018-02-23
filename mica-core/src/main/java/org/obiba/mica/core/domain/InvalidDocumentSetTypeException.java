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

public class InvalidDocumentSetTypeException extends RuntimeException{

  private static final long serialVersionUID = -357777695158511379L;

  public InvalidDocumentSetTypeException(String message) {
    super(message);
  }

  public static InvalidDocumentSetTypeException forSet(DocumentSet documentSet, String expected) {
    return new InvalidDocumentSetTypeException("Document set with id '" + documentSet.getId() + "' has invalid type: " + documentSet.getType() + "(" + expected + " expected)");
  }

}
