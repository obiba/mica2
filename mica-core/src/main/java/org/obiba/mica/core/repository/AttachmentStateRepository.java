package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.file.AttachmentState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AttachmentStateRepository extends MongoRepository<AttachmentState, String> {

  @Query("{'path': {$regex: ?0}}")
  List<AttachmentState> findByPath(String pathRegEx);

  List<AttachmentState> findByPathAndName(String path, String name);
}
