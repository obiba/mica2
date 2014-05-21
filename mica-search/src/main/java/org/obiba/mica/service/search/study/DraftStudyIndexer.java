package org.obiba.mica.service.search.study;

import org.obiba.mica.service.search.AbstractIndexer;
import org.obiba.mica.service.study.event.DraftStudyUpdatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class DraftStudyIndexer extends AbstractIndexer {

  @Override
  protected String getIndexName() {
    return "study-draft";
  }

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
    index(event.getPersistable());
  }

}
