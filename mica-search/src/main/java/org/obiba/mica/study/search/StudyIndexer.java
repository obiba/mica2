/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import com.google.common.eventbus.Subscribe;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.event.*;
import org.obiba.mica.study.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(StudyIndexer.class);

  @Inject
  private Indexer indexer;

  @Inject
  private StudyService studyService;

  @Inject
  private PersonService personService;

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    log.info("Study {} was updated", event.getPersistable());
    indexer.index(Indexer.DRAFT_STUDY_INDEX, (Indexable) addMemberships(event.getPersistable()));
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    log.info("Study {} was published", event.getPersistable());
    indexer.index(Indexer.PUBLISHED_STUDY_INDEX, (Indexable) addMemberships(event.getPersistable()));
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    log.info("Study {} was unpublished", event.getPersistable());
    indexer.delete(Indexer.PUBLISHED_STUDY_INDEX, (Indexable) event.getPersistable());
    indexer.index(Indexer.DRAFT_STUDY_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    log.info("Study {} was deleted", event.getPersistable());
    indexer.delete(Indexer.DRAFT_STUDY_INDEX, (Indexable) event.getPersistable());
    indexer.delete(Indexer.PUBLISHED_STUDY_INDEX, (Indexable) event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexStudies(IndexStudiesEvent event) {
    List<String> studyIds = event.getIds();

    if (studyIds.isEmpty()) {
      reIndexAllPublished(addMemberships(studyService.findAllPublishedStudies()));
      reIndexAllDraft(addMemberships(studyService.findAllDraftStudies()));
    } else {
      // indexAll does not deletes the index before
      indexer.indexAllIndexables(Indexer.PUBLISHED_STUDY_INDEX, addMemberships(studyService.findAllPublishedStudies(studyIds)));
      indexer.indexAllIndexables(Indexer.DRAFT_STUDY_INDEX, addMemberships(studyService.findAllDraftStudies(studyIds)));
    }
  }

  private void reIndexAllDraft(Iterable<BaseStudy> studies) {
    reIndexAll(Indexer.DRAFT_STUDY_INDEX, studies);
  }

  private void reIndexAllPublished(Iterable<BaseStudy> studies) {
    reIndexAll(Indexer.PUBLISHED_STUDY_INDEX, studies);
  }

  private void reIndexAll(String indexName, Iterable<BaseStudy> studies) {
    indexer.reIndexAllIndexables(indexName, studies);
  }

  private List<BaseStudy> addMemberships(List<BaseStudy> studies) {
    return studies.stream().map(this::addMemberships).collect(Collectors.toList());
  }

  private BaseStudy addMemberships(BaseStudy study) {
    Map<String, List<Membership>> membershipMap = personService.getStudyMembershipMap(study.getId());
    personService.setMembershipOrder(study.getMembershipSortOrder(), membershipMap);
    study.setMemberships(membershipMap);

    return study;
  }
}
