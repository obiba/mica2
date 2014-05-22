package org.obiba.mica.service.search.study;

import javax.inject.Inject;

import org.obiba.mica.service.search.ElasticSearchIndexer;
import org.obiba.mica.service.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.service.study.event.StudyPublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class StudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(StudyIndexer.class);

  private static final String DRAFT_STUDY_INDEX = "study-draft";

  private static final String PUBLISHED_STUDY_INDEX = "study-published";

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
    elasticSearchIndexer.index(DRAFT_STUDY_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    log.info("Study {} was published", event.getPersistable());
    elasticSearchIndexer.index(PUBLISHED_STUDY_INDEX, event.getPersistable());
  }

}
