package org.obiba.mica.service.config;

import org.obiba.mica.domain.MicaConfig;
import org.obiba.mica.event.PersistableUpdatedEvent;

public class MicaConfigUpdatedEvent extends PersistableUpdatedEvent<MicaConfig> {

  public MicaConfigUpdatedEvent(MicaConfig micaConfig) {
    super(micaConfig);
  }
}
