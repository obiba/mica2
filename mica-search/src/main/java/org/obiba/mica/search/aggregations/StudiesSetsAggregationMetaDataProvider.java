package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.StudiesSetsAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StudiesSetsAggregationMetaDataProvider extends SetsAggregationMetaDataProvider {

  @Inject
  public StudiesSetsAggregationMetaDataProvider(StudiesSetsAggregationMetaDataHelper helper) {
    super(helper);
  }

}
