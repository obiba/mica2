package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DataAccessEntityRepository<T extends DataAccessEntity> extends MongoRepository<T, String> {
  List<T> findByApplicant(String applicant);
}
