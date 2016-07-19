package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.ProjectConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectConfigRepository extends MongoRepository<ProjectConfig, String> {}
