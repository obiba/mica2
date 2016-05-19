package org.obiba.mica.project;

import org.obiba.mica.project.domain.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the {@link Project} entity.
 */
public interface ProjectRepository extends MongoRepository<Project, String> {

}
