package org.obiba.mica.search.aggregations;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.search.aggregations.helper.NetworkIdAggregationMetaDataHelper;
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
      ? MetaData.newBuilder().title(networks.get(termKey).getTitle().get(locale))
      .description(networks.get(termKey).getDescription().get(locale)).build()
      : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }
}
