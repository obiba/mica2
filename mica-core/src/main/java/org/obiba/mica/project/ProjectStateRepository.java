package org.obiba.mica.project;

import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.project.domain.ProjectState;

/**
 * Spring Data MongoDB repository for the {@link ProjectState} entity.
 */
public interface ProjectStateRepository extends EntityStateRepository<ProjectState> {

}
