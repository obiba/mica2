package org.obiba.mica.study;

import java.util.List;

import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface HarmonizationStudyRepository
  extends MongoRepository<HarmonizationStudy, String>, HarmonizationStudyRepositoryCustom {

  @Query(value = "{'_id' : { $in: ?0 }}", fields = "{_id : 1}")
  List<Study> findAllExistingIds(Iterable<String> ids);
}
