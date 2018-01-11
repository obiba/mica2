/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;

public class TaxonomyIndexable extends TaxonomyEntityIndexable<Taxonomy> {

  private final Taxonomy taxonomy;


  public TaxonomyIndexable(TaxonomyTarget target, Taxonomy taxonomy) {
    super(target);
    this.taxonomy = taxonomy;
  }

  @Override
  public String getId() {
    return getName();
  }



  @Override
  public String getMappingName() {
    return Indexer.TAXONOMY_TYPE;
  }

  @Override
  public String getParentId() {
    return null;
  }

  @Override
  protected Taxonomy getTaxonomyEntity() {
    return taxonomy;
  }
}

