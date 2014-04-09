package org.obiba.mica.event;

import org.obiba.mica.domain.Network;

public class NetworkUpdatedEvent extends EntityUpdatedEvent {

  public NetworkUpdatedEvent(Network network) {
    super(network);
  }
}
