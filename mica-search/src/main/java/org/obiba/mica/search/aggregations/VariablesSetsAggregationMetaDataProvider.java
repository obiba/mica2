package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.VariablesSetsAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class VariablesSetsAggregationMetaDataProvider extends SetsAggregationMetaDataProvider {

  @Inject
  public VariablesSetsAggregationMetaDataProvider(VariablesSetsAggregationMetaDataHelper helper) {
    super(helper);
  }

}
