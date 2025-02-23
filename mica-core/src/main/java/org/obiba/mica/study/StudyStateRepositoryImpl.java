package org.obiba.mica.study;

import org.obiba.mica.study.domain.StudyState;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class StudyStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public StudyStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, StudyState.class.getSimpleName());
  }
}
