package org.obiba.mica.micaConfig.event;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.event.PersistableUpdatedEvent;
import org.obiba.mica.micaConfig.domain.MicaConfig;

import com.google.common.collect.Lists;

public class MicaConfigUpdatedEvent extends PersistableUpdatedEvent<MicaConfig> {

  private List<String> removedRoles = Lists.newArrayList();

  public MicaConfigUpdatedEvent(MicaConfig micaConfig, @NotNull List<String> removedRoles) {
    super(micaConfig);
    this.removedRoles = removedRoles;
  }

  public List<String> getRemovedRoles() {
    return removedRoles;
  }
}
