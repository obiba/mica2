package org.obiba.mica.search.aggregations;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Component;

@Component
public class DataCollectionEventAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  PublishedStudyService publishedStudyService;

  private Map<String, LocalizedString> cache;

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return aggregation.equals("dceIds")
      ? MetaData.newBuilder().title(cache.get(parseDataCollectionEventUId(termKey)).get(locale)).description("").build()
      : null;
  }

  @Override
  public void refresh() {
//    cache = publishedStudyService.findAll()
//      .stream().map(Study::getPopulations).flatMap((pops) -> pops.stream()).map(Population::getDataCollectionEvents)
//      .flatMap((dces) -> dces.stream())
//      .collect();
  }

  private String parseDataCollectionEventUId(String uid) {
    Pattern pattern = Pattern.compile("[^:.*]+$");
    Matcher matcher = pattern.matcher(uid);
    return matcher.find() ? matcher.group() : uid;
  }

}
