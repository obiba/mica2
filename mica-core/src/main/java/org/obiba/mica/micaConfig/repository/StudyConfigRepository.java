package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudyConfigRepository extends MongoRepository<StudyConfig, String> {}
