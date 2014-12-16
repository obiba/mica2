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

  private Map<String, LocalizedString> cache;

  public void refresh() {
    cache = publishedStudyService.findAll().stream().collect(Collectors.toMap(Study::getId, Study::getName));
  }

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    MetaData metaData = null;
    if ("studyIds".equals(aggregation)) {
      metaData = MetaData.newBuilder().title(cache.get(termKey).get(locale)).description("").build();
    }

    return metaData;
  }

}
