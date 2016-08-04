package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.NetworkConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NetworkConfigRepository extends MongoRepository<NetworkConfig, String> {}
