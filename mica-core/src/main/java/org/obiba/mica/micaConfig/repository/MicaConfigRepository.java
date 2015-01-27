package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MicaConfigRepository extends MongoRepository<MicaConfig, String> {

}
