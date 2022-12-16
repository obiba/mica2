
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
import java.util.Map;

public class VariableTaxonomiesUpdatedEvent {

  private Map<String, Taxonomy> taxonomies;

  public VariableTaxonomiesUpdatedEvent(Map<String, Taxonomy> taxonomies) {
    this.taxonomies = taxonomies;
  }

  public List<Taxonomy> getTaxonomies() {
    return new ArrayList<>(taxonomies.values());
  }
}
