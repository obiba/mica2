package org.obiba.mica.micaConfig.event;

import org.obiba.mica.core.event.PersistableUpdatedEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;

public class MicaConfigUpdatedEvent extends PersistableUpdatedEvent<MicaConfig> {

  public MicaConfigUpdatedEvent(MicaConfig micaConfig) {
    super(micaConfig);
  }
}
