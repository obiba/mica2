package org.obiba.mica.event;

import org.obiba.mica.domain.Entity;

public class EntityUpdatedEvent {

  private final Entity entity;

  public EntityUpdatedEvent(Entity entity) {this.entity = entity;}

  public Entity getEntity() {
    return entity;
  }
}
