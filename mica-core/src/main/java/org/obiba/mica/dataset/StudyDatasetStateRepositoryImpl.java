package org.obiba.mica.dataset;

import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.study.EntityStateRepositoryImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StudyDatasetStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public StudyDatasetStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, StudyDatasetState.class.getSimpleName());
  }
}
