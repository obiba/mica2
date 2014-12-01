package org.obiba.mica.micaConfig;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OpalCredentialRepository extends MongoRepository<OpalCredential, String> { }
