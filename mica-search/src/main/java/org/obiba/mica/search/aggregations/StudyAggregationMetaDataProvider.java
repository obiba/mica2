package org.obiba.mica.search.aggregations;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class StudyAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "studyIds";

  @Inject
  AggregationMetaDataHelper helper;

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

  @Component
  public static class AggregationMetaDataHelper {

    @Inject
    PublishedStudyService publishedStudyService;

    @Cacheable(value="aggregations-metadata", key = "'study'")
    public Map<String, LocalizedMetaData> getStudies() {
      List<Study> studies = publishedStudyService.findAll();
      return studies.stream().collect(Collectors
        .toMap(s -> s.getId(), r -> new LocalizedMetaData(r.getName(), r.getAcronym())));
    }
  }
}
