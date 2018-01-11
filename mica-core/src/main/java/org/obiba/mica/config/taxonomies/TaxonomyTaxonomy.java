/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.config.taxonomies;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

@ConfigurationProperties(locations = "classpath:/taxonomies/mica-taxonomy.yml")
public class TaxonomyTaxonomy extends Taxonomy {

  private static final long serialVersionUID = 1L;

  public TaxonomyTaxonomy() {
    setVocabularies(Lists.newArrayList()); //explicit initialization for yaml bean factory
  }

}
