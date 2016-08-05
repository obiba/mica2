package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DataCollectionEventConfigRepository extends MongoRepository<DataCollectionEventConfig, String> {}
