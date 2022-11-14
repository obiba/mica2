package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DataAccessPreliminaryFormRepository extends MongoRepository<DataAccessPreliminaryForm, String> {

  DataAccessPreliminaryForm findFirstByRevision(int revision);

}
