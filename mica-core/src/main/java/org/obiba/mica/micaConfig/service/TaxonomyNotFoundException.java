/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

public class TaxonomyNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 4661208635995765840L;

  private String taxonomyName;

  public TaxonomyNotFoundException() {
    super();
  }

  public TaxonomyNotFoundException(String tentativeTaxonomyName) {
    super(String.format("The taxonomy \"%s\" is not found.", tentativeTaxonomyName));
    taxonomyName = tentativeTaxonomyName;
  }

  public String getTaxonomyName() {
    return taxonomyName;
  }
}
