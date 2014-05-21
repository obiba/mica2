package org.obiba.mica.service.search.study;

import org.obiba.mica.service.search.AbstractIndexer;
import org.obiba.mica.service.study.event.StudyPublishedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class PublishedStudyIndexer extends AbstractIndexer {

  @Override
  protected String getIndexName() {
    return "study-published";
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    log.info("Study {} was published", event.getPersistable());
    index(event.getPersistable());
  }

}
