/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class AggregationMetaDataResolver implements AggregationMetaDataProvider {
  @Inject
  private TaxonomyAggregationMetaDataProvider taxonomyAggregationTitleProvider;

  @Inject
  private DefaultAggregationMetaDataProvider defaultAggregationTitleProvider;

  @Inject
  private DatasetAggregationMetaDataProvider datasetAggregationMetaDataProvider;

  @Override
  public MetaData getTitle(String aggregation, String termKey, String locale) {
    Optional<MetaData> title = Stream
      .of(taxonomyAggregationTitleProvider, datasetAggregationMetaDataProvider, defaultAggregationTitleProvider)
      .map(provider -> provider.getTitle(aggregation, termKey, locale)).filter(metaData -> metaData != null)
      .findFirst();

    return title.get();
  }

  @Override
  public void refresh() {
    Stream.of(taxonomyAggregationTitleProvider, datasetAggregationMetaDataProvider, defaultAggregationTitleProvider)
      .forEach(AggregationMetaDataProvider::refresh);
  }
}
