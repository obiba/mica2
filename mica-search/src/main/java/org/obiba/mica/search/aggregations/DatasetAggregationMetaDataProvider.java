/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.DatasetIdAggregationMetaDataHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final Logger log = LoggerFactory.getLogger(DatasetAggregationMetaDataProvider.class);
  private static final String AGGREGATION_NAME = "datasetId";

  @Inject
  DatasetIdAggregationMetaDataHelper helper;

  @Override
  public void refresh() {
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> datasetsDictionary = helper.getDatasets();

    return AGGREGATION_NAME.equals(aggregation) && datasetsDictionary.containsKey(termKey)
      && datasetsDictionary.get(termKey).getTitle() != null
      ? MetaData.newBuilder()
        .title(datasetsDictionary.get(termKey).getTitle().get(locale))
        .description(datasetsDictionary.get(termKey).getDescription().get(locale))
        .className(datasetsDictionary.get(termKey).getClassName()).build()
      : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

}
