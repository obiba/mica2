package org.obiba.mica.file;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TempFileRepository extends MongoRepository<TempFile, String> {

}
