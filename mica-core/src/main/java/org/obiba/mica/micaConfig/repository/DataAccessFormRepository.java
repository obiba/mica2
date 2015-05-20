package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DataAccessFormRepository extends MongoRepository<DataAccessForm, String> {}
