/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica;

import java.util.NoSuchElementException;

import jakarta.validation.constraints.NotNull;

public class NoSuchEntityException extends NoSuchElementException {

  public NoSuchEntityException(String message){
    super(message);
  }

  public static NoSuchEntityException withId(@NotNull Class clazz, String id) {
    return new NoSuchEntityException(String.format("%s with id %s not found.", clazz.getSimpleName(), id));
  }

  public static NoSuchEntityException withPath(@NotNull Class clazz, String path) {
    return new NoSuchEntityException(String.format("%s with path %s not found.", clazz.getSimpleName(), path));
  }
}
