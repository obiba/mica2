package org.obiba.mica.project;

import org.obiba.mica.project.domain.Project;
import org.obiba.mica.study.EntityStateRepositoryImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ProjectStateRepositoryImpl extends EntityStateRepositoryImpl {

  @Inject
  public ProjectStateRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate, Project.class.getSimpleName());
  }
}
