package org.obiba.mica.event;

import org.springframework.data.domain.Persistable;

public class PersistableDeletedEvent<TPersistable extends Persistable<?>> {

  private final TPersistable persistable;

  public PersistableDeletedEvent(TPersistable persistable) {this.persistable = persistable;}

  public TPersistable getPersistable() {
    return persistable;
  }
}
