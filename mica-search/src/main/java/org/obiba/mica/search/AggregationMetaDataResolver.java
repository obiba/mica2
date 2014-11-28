/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class AggregationMetaDataResolver implements AggregationMetaDataProvider {
  @Inject
  private TaxonomyAggregationMetaDataProvider taxonomyAggregationTitleProvider;

  @Inject
  private DefaultAggregationMetaDataProvider defaultAggregationTitleProvider;

  @Override
  public MetaData getTitle(String aggregation, String termKey, String locale) {
    MetaData title = taxonomyAggregationTitleProvider.getTitle(aggregation, termKey, locale);
    return title == null ? defaultAggregationTitleProvider.getTitle(aggregation, termKey, locale) : title;
  }
}
