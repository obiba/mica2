package org.obiba.mica.core.event;

import org.springframework.data.domain.Persistable;

public class PersistableUpdatedEvent<TPersistable extends Persistable<?>> {

  private final TPersistable persistable;

  public PersistableUpdatedEvent(TPersistable persistable) {this.persistable = persistable;}

  public TPersistable getPersistable() {
    return persistable;
  }
}
