package org.obiba.mica.service.search.study;

import org.obiba.mica.domain.Study;
import org.obiba.mica.service.search.AbstractIndexer;
import org.obiba.mica.service.study.event.DraftStudyUpdatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class DraftStudyIndexer extends AbstractIndexer {

  private static final String INDEX_NAME = "study-draft";

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
    index(event.getPersistable());
  }

  public void index(Study study) {

  }

  @Override
  protected String getIndexName() {
    return INDEX_NAME;
  }
}
