/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Indexable;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.event.StudyDeletedEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.obiba.mica.study.service.CollectionStudyService;
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

  public static final String COLLECTION_STUDY_TYPE = "Study";

  public static final String HARMONIZATION_STUDY_TYPE = "HarmonizationStudy";

  public static final String[] LOCALIZED_ANALYZED_FIELDS = {"acronym", "name", "objectives"};

  public static final String DEFAULT_SORT_FIELD  = "name";

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Inject
  private CollectionStudyService collectionStudyService;

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
    elasticSearchIndexer.index(DRAFT_STUDY_INDEX, (Indexable)event.getPersistable());
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    log.info("Study {} was published", event.getPersistable());
    elasticSearchIndexer.index(PUBLISHED_STUDY_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.info("Study {} was unpublished", event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_STUDY_INDEX, (Indexable)event.getPersistable());
    elasticSearchIndexer.index(DRAFT_STUDY_INDEX, (Indexable)event.getPersistable());
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    log.info("Study {} was deleted", event.getPersistable());
    elasticSearchIndexer.delete(DRAFT_STUDY_INDEX, (Indexable)event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_STUDY_INDEX, (Indexable)event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexStudies(IndexStudiesEvent event) {
    reIndexAllPublished(collectionStudyService.findAllPublishedStudies());
    reIndexAllDraft(collectionStudyService.findAllDraftStudies());
  }

  public void reIndexAllDraft(Iterable<Study> studies) {
    reIndexAll(DRAFT_STUDY_INDEX, studies);
  }

  public void reIndexAllPublished(Iterable<Study> studies) {
    reIndexAll(PUBLISHED_STUDY_INDEX, studies);
  }

  private void reIndexAll(String indexName, Iterable<Study> studies) {
    elasticSearchIndexer.reindexAll(indexName, studies);
  }
}
