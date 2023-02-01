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

import java.util.List;

/**
 * Variable description, for summary statistics.
 */
public interface IVariable {

  /**
   * Get the variable name.
   *
   * @return
   */
  String getName();

  /**
   * Get the value type of the variable.
   *
   * @return
   */
  String getValueType();

  /**
   * Whether the variable has categories (may still be categorical without, see boolean value type).
   *
   * @return
   */
  boolean hasCategories();

  /**
   * Helper to get the category names.
   *
   * @return
   */
  List<String> getCategoryNames();

  /**
   * Get a category by its name.
   *
   * @param name
   * @return
   */
  ICategory getCategory(String name);
}
