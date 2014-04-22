package org.obiba.mica.event;

import org.springframework.data.domain.Persistable;

public class PersistableUpdatedEvent {

  private final Persistable<?> persistable;

  public PersistableUpdatedEvent(Persistable<?> persistable) {this.persistable = persistable;}

  public Persistable<?> getPersistable() {
    return persistable;
  }
}
