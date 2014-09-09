package org.obiba.mica.study.search;

import javax.inject.Inject;

import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class StudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(StudyIndexer.class);

  public static final String DRAFT_STUDY_INDEX = "study-draft";

  public static final String PUBLISHED_STUDY_INDEX = "study-published";

  public static final String STUDY_TYPE = "Study";

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

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    log.info("Study {} was deleted", event.getPersistable());
    elasticSearchIndexer.delete(DRAFT_STUDY_INDEX, event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_STUDY_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexStudies(IndexStudiesEvent event) {
    reIndexAllPublished(event.getPublishedStudies());
    reIndexAllDraft(event.getDraftStudies());
  }

  @Async
  public void reIndexAllDraft(Iterable<Study> studies) {
    reIndexAll(DRAFT_STUDY_INDEX, studies);
  }

  @Async
  public void reIndexAllPublished(Iterable<Study> studies) {
    reIndexAll(PUBLISHED_STUDY_INDEX, studies);
  }

  private void reIndexAll(String indexName, Iterable<Study> studies) {
    if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    elasticSearchIndexer.indexAll(indexName, studies);
  }
}
