package org.obiba.mica.search.aggregations;

import java.util.Map;
import javax.inject.Inject;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.SetsAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

@Component
public class SetsAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "sets";

  private SetsAggregationMetaDataHelper helper;

  @Inject
  public SetsAggregationMetaDataProvider(SetsAggregationMetaDataHelper helper) {
    this.helper = helper;
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> setIds = helper.getSetIds();

    return AGGREGATION_NAME.equals(aggregation) && setIds.containsKey(termKey) ?
      MetaData.newBuilder()
        .title(setIds.get(termKey).getTitle().get(locale))
        .className(setIds.get(termKey).getClassName())
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
