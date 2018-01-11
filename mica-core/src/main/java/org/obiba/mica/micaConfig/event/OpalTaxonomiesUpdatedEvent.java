
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

import org.obiba.opal.core.domain.taxonomy.Taxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class OpalTaxonomiesUpdatedEvent {

  private ConcurrentMap<String, Taxonomy> opalTaxonomies;

  public OpalTaxonomiesUpdatedEvent(ConcurrentMap<String, Taxonomy> opalTaxonomies) {
    this.opalTaxonomies = opalTaxonomies;
  }

  public List<Taxonomy> extractOpalTaxonomies() {
    return new ArrayList<>(opalTaxonomies.values());
  }

  public boolean hasOpalTaxonomies() {
    return opalTaxonomies != null;
  }
}
