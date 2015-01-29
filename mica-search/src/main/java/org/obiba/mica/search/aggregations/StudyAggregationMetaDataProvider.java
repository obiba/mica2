package org.obiba.mica.search.aggregations;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.stereotype.Component;

@Component
public class StudyAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  PublishedStudyService publishedStudyService;

  private Map<String, LocalizedString> cacheTitles;

  private Map<String, LocalizedString> cacheDescriptions;

  public void refresh() {
    cacheTitles = publishedStudyService.findAll().stream().collect(Collectors.toMap(Study::getId, Study::getAcronym));
    cacheDescriptions = publishedStudyService.findAll().stream()
      .collect(Collectors.toMap(Study::getId, Study::getName));
  }

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return "studyIds".equals(aggregation) && cacheTitles.containsKey(termKey)
      ? MetaData.newBuilder().title(cacheTitles.get(termKey).get(locale))
      .description(cacheDescriptions.get(termKey).get(locale)).build()
      : null;
  }

}
