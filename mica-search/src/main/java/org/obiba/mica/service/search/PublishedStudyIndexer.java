package org.obiba.mica.service.search;

import org.obiba.mica.service.study.event.StudyPublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class PublishedStudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(PublishedStudyIndexer.class);

  @Async
  @Subscribe
  public void studyUpdated(StudyPublishedEvent event) {
    log.info("Study {} was published", event.getPersistable());
  }

}
