package org.obiba.mica.event;

import org.obiba.mica.domain.MicaConfig;

public class MicaConfigUpdatedEvent extends PersistableUpdatedEvent {

  public MicaConfigUpdatedEvent(MicaConfig micaConfig) {
    super(micaConfig);
  }
}
