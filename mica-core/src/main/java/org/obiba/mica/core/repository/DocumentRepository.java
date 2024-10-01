package org.obiba.mica.core.repository;

import org.obiba.mica.spi.search.Identified;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface DocumentRepository<T extends Identified> {

  long count();

  Page<T> findByIdIn(Collection<String> ids, Pageable pageable);
}
