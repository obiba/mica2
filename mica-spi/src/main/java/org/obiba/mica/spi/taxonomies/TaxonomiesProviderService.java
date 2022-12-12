/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.taxonomies;

import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.plugins.spi.ServicePlugin;

import java.util.List;

public interface TaxonomiesProviderService extends ServicePlugin {

  boolean isFor(TaxonomyTarget target);

  List<Taxonomy> getTaxonomies();

}
