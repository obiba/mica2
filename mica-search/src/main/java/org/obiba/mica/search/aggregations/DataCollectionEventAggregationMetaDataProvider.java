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

import jakarta.inject.Inject;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.DceIdAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

@Component
public class DataCollectionEventAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "dceId";

  @Inject
  DceIdAggregationMetaDataHelper helper;

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, AggregationMetaDataProvider.LocalizedMetaData> dceDictionary = helper.getDces();

    return AGGREGATION_NAME.equals(aggregation) && dceDictionary.containsKey(termKey) ? MetaData.newBuilder() //
      .title(dceDictionary.get(termKey).getTitle().get(locale)) //
      .description(dceDictionary.get(termKey).getDescription().get(locale)) //
      .className(dceDictionary.get(termKey).getClassName()) //
      .start(dceDictionary.get(termKey).getStart()) //
      .end(dceDictionary.get(termKey).getEnd()) //
      .sortField(dceDictionary.get(termKey).getSortField())
      .build() : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

  @Override
  public void refresh() {
  }

}
