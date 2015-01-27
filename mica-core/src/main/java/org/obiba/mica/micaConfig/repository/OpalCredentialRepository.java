package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OpalCredentialRepository extends MongoRepository<OpalCredential, String> { }
