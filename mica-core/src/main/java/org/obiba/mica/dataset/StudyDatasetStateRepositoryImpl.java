package org.obiba.mica.dataset;

import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.study.EntityStateRepositoryImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class StudyDatasetStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public StudyDatasetStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, StudyDatasetState.class.getSimpleName());
  }

  @Override
  protected String getDefaultAggregationCounts() {
    return super.getDefaultAggregationCounts()
      + ",\"requireIndexing\": {\n" +
      "    \"$sum\": {\n" +
      "      \"$cond\": [{\"$and\": [{\"$ifNull\": [\"$requireIndexing\", false ]}, {\"$eq\": [\"$requireIndexing\", true ] } ] }, 1, 0 ]\n" +
      "    }\n" +
      "  }";
  }
}
