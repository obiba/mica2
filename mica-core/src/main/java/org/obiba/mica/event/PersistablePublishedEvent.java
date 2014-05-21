package org.obiba.mica.event;

import org.springframework.data.domain.Persistable;

public class PersistablePublishedEvent<TPersistable extends Persistable<?>>
    extends PersistableUpdatedEvent<TPersistable> {

  public PersistablePublishedEvent(TPersistable persistable) {
    super(persistable);
  }
}
