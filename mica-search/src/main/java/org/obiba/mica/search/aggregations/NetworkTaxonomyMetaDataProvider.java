/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import javax.inject.Inject;

import org.obiba.mica.search.aggregations.helper.NetworkIdAggregationMetaDataHelper;
import org.obiba.mica.search.aggregations.helper.StudyIdAggregationMetaDataHelper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Component;

@Component
public class NetworkTaxonomyMetaDataProvider extends ConfigurationTaxonomyMetaDataProvider {

  @Inject
  private NetworkIdAggregationMetaDataHelper networkHelper;

  @Inject
  private StudyIdAggregationMetaDataHelper studyHelper;

  @Override
  protected Taxonomy getTaxonomy() {
    Taxonomy taxonomy = micaConfigService.getNetworkTaxonomy();
    networkHelper.addIdTerms(taxonomy, "id");
    studyHelper.addIdTerms(taxonomy, "studyIds");
    return taxonomy;
  }
}
