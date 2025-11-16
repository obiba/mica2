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

public class InvalidFileNameException extends NoSuchElementException {

  private static final long serialVersionUID = 868816504209184465L;

  @NotNull
  private final String name;

  public InvalidFileNameException(@NotNull String name) {
    super("File name cannot contain these characters '$/%#'");
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
