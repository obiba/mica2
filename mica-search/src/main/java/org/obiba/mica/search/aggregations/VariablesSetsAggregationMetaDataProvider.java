package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.VariablesSetsAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class VariablesSetsAggregationMetaDataProvider extends SetsAggregationMetaDataProvider {

  @Inject
  public VariablesSetsAggregationMetaDataProvider(VariablesSetsAggregationMetaDataHelper helper) {
    super(helper);
  }

}
