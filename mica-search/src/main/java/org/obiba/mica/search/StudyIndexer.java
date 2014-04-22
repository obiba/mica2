package org.obiba.mica.search;

import org.obiba.mica.event.StudyUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class StudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(StudyIndexer.class);

  @Async
  @Subscribe
  public void studyUpdated(StudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
  }

}
