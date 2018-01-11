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
import org.obiba.mica.micaConfig.service.helper.NetworkIdAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

@Component
public class NetworkAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "networkId";

  @Inject
  NetworkIdAggregationMetaDataHelper helper;

  @Override
  public void refresh() {
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> networks = helper.getNetworks();
    return AGGREGATION_NAME.equals(aggregation) && networks.containsKey(termKey)
      ? MetaData.newBuilder()
        .title(networks.get(termKey).getTitle().get(locale))
        .description(networks.get(termKey).getDescription().get(locale))
        .className(networks.get(termKey).getClassName()).build()
      : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }
}
