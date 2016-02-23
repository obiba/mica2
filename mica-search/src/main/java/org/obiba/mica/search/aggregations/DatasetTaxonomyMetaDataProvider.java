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

import org.obiba.mica.search.aggregations.helper.DatasetIdAggregationMetaDataHelper;
import org.obiba.mica.search.aggregations.helper.NetworkIdAggregationMetaDataHelper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Component;

@Component
public class DatasetTaxonomyMetaDataProvider extends ConfigurationTaxonomyMetaDataProvider {

  @Inject
  private DatasetIdAggregationMetaDataHelper datasetHelper;

  @Inject
  NetworkIdAggregationMetaDataHelper networkHelper;

  @Override
  protected Taxonomy getTaxonomy() {
    Taxonomy taxonomy = taxonomyService.getDatasetTaxonomy();
    datasetHelper.applyIdTerms(taxonomy, "id");
    networkHelper.applyIdTerms(taxonomy, "networkId");
    return taxonomy;
  }
}
