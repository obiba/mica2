/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

public enum TaxonomyTarget {
  VARIABLE, STUDY, NETWORK, DATASET, TAXONOMY;

  public String asId() {
    return asId(this);
  }

  public static String asId(TaxonomyTarget value) {
    return value.toString().toLowerCase();
  }

  public static TaxonomyTarget fromId(String id) {
    return valueOf(id.toUpperCase());
  }}
