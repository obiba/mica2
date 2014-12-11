package org.obiba.mica.study;

import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Study entity.
 */

public interface StudyRepository extends MongoRepository<Study, String> {
}
