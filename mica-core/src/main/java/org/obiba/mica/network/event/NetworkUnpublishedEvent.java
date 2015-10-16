package org.obiba.mica.network.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.Study;

public class NetworkUnpublishedEvent extends PersistablePublishedEvent<Network> {

  public NetworkUnpublishedEvent(Network network) {
    super(network);
  }
}
