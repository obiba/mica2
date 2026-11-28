package org.obiba.mica.search.aggregations;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.SetsAggregationMetaDataHelper;

import java.util.Map;

abstract class SetsAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "sets";

  private final SetsAggregationMetaDataHelper helper;

  // the set titles, read once per query (see refresh()) instead of once per aggregation bucket
  private volatile Map<String, LocalizedMetaData> setIds;

  protected SetsAggregationMetaDataProvider(SetsAggregationMetaDataHelper helper) {
    this.helper = helper;
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    if (!AGGREGATION_NAME.equals(aggregation)) return null;

    // local copy: a concurrent query may reset the field in refresh()
    Map<String, LocalizedMetaData> ids = setIds;
    if (ids == null) {
      ids = helper.getSetIds();
      setIds = ids;
    }

    LocalizedMetaData md = ids.get(termKey);
    return md == null ? null : MetaData.newBuilder()
      .title(md.getTitle().get(locale))
      .className(md.getClassName())
      .build();
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

  @Override
  public void refresh() {
    setIds = null;
  }
}
