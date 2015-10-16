package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.core.domain.EntityState;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface EntityStateRepository<T extends EntityState> extends MongoRepository<T, String> {
  List<T> findByPublishedTagNotNull();
}
