package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.file.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AttachmentRepository extends MongoRepository<Attachment, String> {
  @Query("{'path': {$regex: ?0}}")
  List<Attachment> findByPath(String pathRegEx);
}
