package org.obiba.mica.micaConfig;

import org.obiba.mica.event.PersistableUpdatedEvent;

public class MicaConfigUpdatedEvent extends PersistableUpdatedEvent<MicaConfig> {

  public MicaConfigUpdatedEvent(MicaConfig micaConfig) {
    super(micaConfig);
  }
}
