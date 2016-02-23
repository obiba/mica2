/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
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
import org.obiba.mica.search.aggregations.helper.DceIdAggregationMetaDataHelper;
import org.obiba.mica.search.aggregations.helper.NetworkIdAggregationMetaDataHelper;
import org.obiba.mica.search.aggregations.helper.StudyIdAggregationMetaDataHelper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Component;

@Component
public class VariableTaxonomyMetaDataProvider extends ConfigurationTaxonomyMetaDataProvider {

  @Inject
  StudyIdAggregationMetaDataHelper studyHelper;

  @Inject
  DatasetIdAggregationMetaDataHelper datasetHelper;

  @Inject
  NetworkIdAggregationMetaDataHelper networkHelper;

  @Inject
  DceIdAggregationMetaDataHelper dceHelper;

  @Override
  protected Taxonomy getTaxonomy() {
    Taxonomy taxonomy = taxonomyService.getVariableTaxonomy();
    studyHelper.applyIdTerms(taxonomy, "studyIds");
    datasetHelper.applyIdTerms(taxonomy, "datasetId");
    networkHelper.applyIdTerms(taxonomy, "networkId");
    dceHelper.applyIdTerms(taxonomy, "dceIds");
    return taxonomy;
  }
}
