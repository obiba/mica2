package org.obiba.mica.core.repository;


public interface DBRefAwareRepository<T> {
  T saveWithReferences(T obj);

  void deleteWithReferences(T obj);
}
