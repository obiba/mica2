package org.obiba.mica.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

@Component
public class DeadEventSubscriber {

  private static final Logger log = LoggerFactory.getLogger(DeadEventSubscriber.class);

  @Subscribe
  public void handleDeadEvent(DeadEvent deadEvent) {
    log.error("Event {} from source {} is not handled", deadEvent.getEvent(), deadEvent.getSource());
  }

}
