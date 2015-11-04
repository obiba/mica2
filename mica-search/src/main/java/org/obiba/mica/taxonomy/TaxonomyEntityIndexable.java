/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import org.obiba.mica.core.domain.Indexable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class TaxonomyEntityIndexable<T extends TaxonomyEntity> implements Indexable {

  @JsonIgnore
  protected abstract T getTaxonomyEntity();

  public String getName() {
    return getTaxonomyEntity().getName();
  }

  public LocalizedString getTitle() {
    return LocalizedString.from(getTaxonomyEntity().getTitle());
  }

  public LocalizedString getDescription() {
    return LocalizedString.from(getTaxonomyEntity().getDescription());
  }

  public LocalizedString getKeywords() {
    return LocalizedString.from(getTaxonomyEntity().getKeywords());
  }

  @Override
  public String getClassName() {
    return null;
  }

}
