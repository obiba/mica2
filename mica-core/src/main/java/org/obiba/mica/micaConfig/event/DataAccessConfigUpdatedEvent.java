package org.obiba.mica.micaConfig.event;

import org.obiba.mica.micaConfig.domain.DataAccessConfig;

public class DataAccessConfigUpdatedEvent {

  private final DataAccessConfig config;

  public DataAccessConfigUpdatedEvent(DataAccessConfig dataAccessConfig) {
    this.config = dataAccessConfig;
  }

  public DataAccessConfig getConfig() {
    return config;
  }
}
