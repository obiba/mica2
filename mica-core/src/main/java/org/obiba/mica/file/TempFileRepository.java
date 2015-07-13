package org.obiba.mica.file;

import java.util.List;

import org.joda.time.DateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TempFileRepository extends MongoRepository<TempFile, String> {
  List<TempFile> findByCreatedDateLessThan(DateTime dateTime, Pageable pageable);
}
