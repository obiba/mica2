package org.obiba.mica.micaConfig.repository;

import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HarmonizationStudyConfigRepository extends MongoRepository<HarmonizationStudyConfig, String> {}
