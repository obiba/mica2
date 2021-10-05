package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DataAccessAmendmentFormRepository extends MongoRepository<DataAccessAmendmentForm, String> {

  DataAccessAmendmentForm findFirstByRevision(int revision);

}
