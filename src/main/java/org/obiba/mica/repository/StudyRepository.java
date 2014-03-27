package org.obiba.mica.repository;

import org.obiba.mica.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Study entity.
 */
public interface StudyRepository extends JpaRepository<Study, Long> {

}
