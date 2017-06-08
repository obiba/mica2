package org.obiba.mica.core.domain;

import org.springframework.data.domain.Persistable;

public interface EntityBase extends Persistable<String>, Timestamped {
  void setId(String id);
}
