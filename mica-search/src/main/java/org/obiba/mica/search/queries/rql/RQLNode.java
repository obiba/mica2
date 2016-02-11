/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.rql;

/**
 * Mica supported RQL node names.
 */
enum RQLNode {
  VARIABLE, //
  DATASET, //
  STUDY, //
  NETWORK, //
  LIMIT, //
  SORT, //
  AND, //
  NAND, //
  OR, //
  NOR, //
  NOT, //
  IN, //
  OUT, //
  EQ, //
  GT, //
  GE, //
  LT, //
  LE, //
  BETWEEN, //
  MATCH, //
  EXISTS, //
  MISSING, //
  FACET, //
  LOCALE, //
  AGGREGATE, //
  BUCKET;

  public static RQLNode getType(String name) {
    return valueOf(name.trim().toUpperCase());
  }
}
