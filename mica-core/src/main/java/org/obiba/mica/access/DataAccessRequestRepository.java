package org.obiba.mica.access;

import java.util.List;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the {@link org.obiba.mica.access.domain.DataAccessRequest} entity.
 */

public interface DataAccessRequestRepository extends MongoRepository<DataAccessRequest, String>,
  DataAccessRequestRepositoryCustom {

  List<DataAccessRequest> findByApplicant(String applicant);

}
