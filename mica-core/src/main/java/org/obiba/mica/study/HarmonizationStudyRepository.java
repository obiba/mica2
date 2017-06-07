package org.obiba.mica.study;

import org.obiba.mica.study.domain.HarmonizationStudy;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HarmonizationStudyRepository extends MongoRepository<HarmonizationStudy, String>, HarmonizationStudyRepositoryCustom {}
