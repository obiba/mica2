package org.obiba.mica.project;

import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.study.EntityStateRepositoryImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class ProjectStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public ProjectStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, ProjectState.class.getSimpleName());
  }
}
