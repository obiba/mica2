package org.obiba.mica.search.aggregations;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.DceIdAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

@Component
public class DataCollectionEventAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "dceIds";

  @Inject
  DceIdAggregationMetaDataHelper helper;

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, AggregationMetaDataProvider.LocalizedMetaData> dceDictionary = helper.getDces();

    return AGGREGATION_NAME.equals(aggregation) && dceDictionary.containsKey(termKey)
      ? MetaData.newBuilder()
          .title(dceDictionary.get(termKey).getTitle().get(locale))
          .description(dceDictionary.get(termKey).getDescription().get(locale)).build()
      : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

  @Override
  public void refresh() {
  }

}
