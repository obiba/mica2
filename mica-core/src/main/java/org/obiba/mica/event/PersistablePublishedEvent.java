package org.obiba.mica.event;

import org.springframework.data.domain.Persistable;

public class PersistablePublishedEvent {

  private final Persistable<?> persistable;

  public PersistablePublishedEvent(Persistable<?> persistable) {this.persistable = persistable;}

  public Persistable<?> getPersistable() {
    return persistable;
  }
}
