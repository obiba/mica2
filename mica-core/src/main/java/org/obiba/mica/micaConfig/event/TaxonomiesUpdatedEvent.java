
/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.event;

import org.obiba.mica.spi.search.TaxonomyTarget;

public class TaxonomiesUpdatedEvent {

  public TaxonomiesUpdatedEvent() {}

  public TaxonomiesUpdatedEvent(String taxonomyName, TaxonomyTarget taxonomyTarget) {
    this.taxonomyName = taxonomyName;
    this.taxonomyTarget = taxonomyTarget;
  }

  private String taxonomyName;

  private TaxonomyTarget taxonomyTarget;

  public String getTaxonomyName() {
    return taxonomyName;
  }

  public TaxonomyTarget getTaxonomyTarget() {
    return taxonomyTarget;
  }
}
