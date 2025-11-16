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

import java.util.NoSuchElementException;

import jakarta.validation.constraints.NotNull;

public class NoSuchTempFileException extends NoSuchElementException {

  private static final long serialVersionUID = 5887330656285998606L;

  @NotNull
  private final String id;

  public NoSuchTempFileException(@NotNull String id) {
    super("No such temp file '" + id + "'");
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
