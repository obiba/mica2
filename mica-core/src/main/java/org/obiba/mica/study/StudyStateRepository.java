package org.obiba.mica.study;

import org.obiba.mica.core.repository.EntityStateRepository;
import org.obiba.mica.study.domain.StudyState;

/**
 * Spring Data MongoDB repository for the StudyState entity.
 */
public interface StudyStateRepository extends EntityStateRepository<StudyState> {
}
