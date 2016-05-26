package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.ProjectForm;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectFormRepository extends MongoRepository<ProjectForm, String> {}
