package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.PopulationIdAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class PopulationAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "populationId";

  private final PopulationIdAggregationMetaDataHelper helper;

  @Inject
  public PopulationAggregationMetaDataProvider(PopulationIdAggregationMetaDataHelper helper) {
    this.helper = helper;
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> dataMap = helper.getPopulations();
    return AGGREGATION_NAME.equals(aggregation) && dataMap.containsKey(termKey) ?
      MetaData.newBuilder()
        .title(dataMap.get(termKey).getTitle().get(locale))
        .description(dataMap.get(termKey).getDescription().get(locale))
        .className(dataMap.get(termKey).getClassName())
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
