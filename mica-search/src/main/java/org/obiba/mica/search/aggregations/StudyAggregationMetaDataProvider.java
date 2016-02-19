package org.obiba.mica.search.aggregations;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.search.aggregations.helper.StudyIdAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

@Component
public class StudyAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "studyIds";

  @Inject
  StudyIdAggregationMetaDataHelper helper;

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> studiesDictionary = helper.getStudies();
    return AGGREGATION_NAME.equals(aggregation) && studiesDictionary.containsKey(termKey)
      ? MetaData.newBuilder().title(studiesDictionary.get(termKey).getTitle().get(locale))
        .description(studiesDictionary.get(termKey).getDescription().get(locale)).build()
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
