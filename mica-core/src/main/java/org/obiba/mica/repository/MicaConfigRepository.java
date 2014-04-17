package org.obiba.mica.repository;

import org.obiba.mica.domain.MicaConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MicaConfigRepository extends MongoRepository<MicaConfig, String> {

}
