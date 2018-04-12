/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search.rql;

/**
 * Mica supported RQL node names.
 */
public enum RQLNode {
  VARIABLE,
  DATASET,
  STUDY,
  NETWORK,
  GENERIC,
  LIMIT,
  SORT,
  AND,
  NAND,
  OR,
  NOR,
  NOT,
  CONTAINS,
  IN,
  OUT,
  EQ,
  GT,
  GE,
  LT,
  LE,
  BETWEEN,
  MATCH,
  LIKE,
  EXISTS,
  MISSING,
  QUERY,
  FACET,
  LOCALE,
  AGGREGATE,
  BUCKET,
  RE,
  SELECT,
  FIELDS,
  FILTER;

  public static RQLNode getType(String name) {
    return valueOf(name.trim().toUpperCase());
  }
}
