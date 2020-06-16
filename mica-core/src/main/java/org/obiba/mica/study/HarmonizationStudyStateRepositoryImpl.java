package org.obiba.mica.study;

import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class HarmonizationStudyStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public HarmonizationStudyStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, HarmonizationStudyState.class.getSimpleName());
  }
}
