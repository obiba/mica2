package org.obiba.mica.jpa.repository;

import org.obiba.mica.jpa.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Study entity.
 */
public interface StudyRepository extends JpaRepository<Study, Long> {

}
