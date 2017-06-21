package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.HarmonizationStudyConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HarmonizationConfigRepository extends MongoRepository<HarmonizationStudyConfig, String> {}
