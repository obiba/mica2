package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DataAccessFeasibilityFormRepository extends MongoRepository<DataAccessFeasibilityForm, String> {

  DataAccessFeasibilityForm findFirstByRevision(int revision);

}
