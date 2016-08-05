package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.PopulationConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PopulationConfigRepository extends MongoRepository<PopulationConfig, String> {}
