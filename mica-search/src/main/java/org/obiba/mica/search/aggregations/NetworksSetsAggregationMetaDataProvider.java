package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.NetworksSetsAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class NetworksSetsAggregationMetaDataProvider extends SetsAggregationMetaDataProvider {

  @Inject
  public NetworksSetsAggregationMetaDataProvider(NetworksSetsAggregationMetaDataHelper helper) {
    super(helper);
  }

}
