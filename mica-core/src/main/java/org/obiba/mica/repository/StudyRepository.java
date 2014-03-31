package org.obiba.mica.repository;

import org.obiba.mica.domain.Study;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Study entity.
 */
public interface StudyRepository extends MongoRepository<Study, String> {

  Study findByName(String name);

}
