package org.obiba.mica.core.repository;

import org.obiba.mica.file.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AttachmentRepository extends MongoRepository<Attachment, String> {}
