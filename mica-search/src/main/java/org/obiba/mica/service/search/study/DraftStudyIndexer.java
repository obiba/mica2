package org.obiba.mica.service.search.study;

import org.obiba.mica.service.study.event.DraftStudyUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class DraftStudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(DraftStudyIndexer.class);

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
  }

}
