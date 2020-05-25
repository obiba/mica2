package org.obiba.mica.dataset;

import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.study.EntityStateRepositoryImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class HarmonizationDatasetStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public HarmonizationDatasetStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, HarmonizationDatasetState.class.getSimpleName());
  }
}
