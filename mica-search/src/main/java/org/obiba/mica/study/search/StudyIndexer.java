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

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.event.DocumentSetDeletedEvent;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.*;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.study.service.StudySetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class StudyIndexer {

  private static final Logger log = LoggerFactory.getLogger(StudyIndexer.class);

  private final Lock lock = new ReentrantLock();

  private final Indexer indexer;

  private final StudyService studyService;

  private final PersonService personService;

  private final StudySetService studySetService;

  private final PublishedDatasetService publishedDatasetService;

  @Inject
  public StudyIndexer(Indexer indexer, StudyService studyService, PersonService personService, StudySetService studySetService, PublishedDatasetService publishedDatasetService) {
    this.indexer = indexer;
    this.studyService = studyService;
    this.personService = personService;
    this.studySetService = studySetService;
    this.publishedDatasetService = publishedDatasetService;
  }

  @Async
  @Subscribe
  public void studyUpdated(DraftStudyUpdatedEvent event) {
    lock.lock();
    try {
      log.info("Study {} was updated", event.getPersistable());
      indexer.index(Indexer.DRAFT_STUDY_INDEX, (Indexable) decorate(event.getPersistable(), true));
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void studyPublished(StudyPublishedEvent event) {
    lock.lock();
    try {
      log.info("Study {} was published", event.getPersistable());
      indexer.index(Indexer.PUBLISHED_STUDY_INDEX, (Indexable) decorate(event.getPersistable(), false));
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void studyUnpublished(StudyUnpublishedEvent event) {
    lock.lock();
    try {
      log.info("Study {} was unpublished", event.getPersistable());
      indexer.delete(Indexer.PUBLISHED_STUDY_INDEX, (Indexable) event.getPersistable());
      indexer.index(Indexer.DRAFT_STUDY_INDEX, (Indexable) event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void studyDeleted(StudyDeletedEvent event) {
    lock.lock();
    try {
      log.info("Study {} was deleted", event.getPersistable());
      indexer.delete(Indexer.DRAFT_STUDY_INDEX, (Indexable) event.getPersistable());
      indexer.delete(Indexer.PUBLISHED_STUDY_INDEX, (Indexable) event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void reIndexStudies(IndexStudiesEvent event) {
    lock.lock();
    try {
      List<String> studyIds = event.getIds();

      if (studyIds.isEmpty()) {
        reIndexAllPublished(decorate(studyService.findAllPublishedStudies(), false));
        reIndexAllDraft(decorate(studyService.findAllDraftStudies(), true));
      } else {
        // indexAll does not delete the index before
        indexer.indexAllIndexables(Indexer.PUBLISHED_STUDY_INDEX, decorate(studyService.findAllPublishedStudies(studyIds), false));
        indexer.indexAllIndexables(Indexer.DRAFT_STUDY_INDEX, decorate(studyService.findAllDraftStudies(studyIds), true));
      }
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void documentSetUpdated(DocumentSetUpdatedEvent event) {
    if (!studySetService.isForType(event.getPersistable())) return;
    lock.lock();
    try {
      List<BaseStudy> toIndex = Lists.newArrayList();
      String id = event.getPersistable().getId();
      if (event.hasRemovedIdentifiers()) {
        List<BaseStudy> toRemove = studySetService.getPublishedStudies(event.getRemovedIdentifiers(), false);
        toRemove.forEach(std -> std.removeSet(id));
        toIndex.addAll(toRemove);
      }
      List<BaseStudy> studies = studySetService.getPublishedStudies(event.getPersistable(), false);
      studies.stream()
        .filter(std -> !std.containsSet(id))
        .forEach(std -> {
          std.addSet(id);
          toIndex.add(std);
        });
      indexer.indexAllIndexables(Indexer.PUBLISHED_STUDY_INDEX, toIndex);
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void documentSetDeleted(DocumentSetDeletedEvent event) {
    if (!studySetService.isForType(event.getPersistable())) return;
    lock.lock();
    try {
      DocumentSet documentSet = event.getPersistable();
      if (!documentSet.getIdentifiers().isEmpty()) {
        List<BaseStudy> toIndex = Lists.newArrayList();
        List<BaseStudy> toRemove = studySetService.getPublishedStudies(event.getPersistable(), false);
        toRemove.forEach(std -> std.removeSet(documentSet.getId()));
        toIndex.addAll(toRemove);
        indexer.indexAllIndexables(Indexer.PUBLISHED_STUDY_INDEX, toIndex);
      }
    } finally {
      lock.unlock();
    }
  }

  //
  // Private methods
  //

  private void reIndexAllDraft(Iterable<BaseStudy> studies) {
    reIndexAll(Indexer.DRAFT_STUDY_INDEX, studies);
  }

  private void reIndexAllPublished(Iterable<BaseStudy> studies) {
    reIndexAll(Indexer.PUBLISHED_STUDY_INDEX, studies);
  }

  private void reIndexAll(String indexName, Iterable<BaseStudy> studies) {
    indexer.reIndexAllIndexables(indexName, studies);
  }

  private List<BaseStudy> addInferredAttributes(List<BaseStudy> studies, boolean draft) {
    studies.forEach(study -> {
      Set<Attribute> inferredAttributes = new HashSet();
      List<String> fields = new ArrayList<String>() {{
        add("inferredAttributes");
      }};

      if (!draft) {
        if (study instanceof Study) {
          publishedDatasetService.find(0, 99999, null, null, study.getId(), null, fields)
            .getList()
            .forEach(dataset -> inferredAttributes.addAll(dataset.getInferredAttributes()));
        } else {
          publishedDatasetService.getHarmonizationDatasetsByStudy(study.getId())
            .forEach(dataset -> inferredAttributes.addAll(dataset.getInferredAttributes()));
        }
      }

      log.debug("Study {} inferred attributes {}", study.getId(), inferredAttributes.size());
      study.setInferredAttributes(inferredAttributes);
    });

    return studies;
  }

  private List<BaseStudy> decorate(List<BaseStudy> studies, boolean draft) {
    return addInferredAttributes(addMemberships(addSets(studies)), draft);
  }

  private BaseStudy decorate(BaseStudy study, boolean draft) {
    return addInferredAttributes(Stream.of(addMemberships(addSets(study))).collect(Collectors.toList()), draft).get(0);
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

  private BaseStudy addSets(BaseStudy study) {
    studySetService.getAll().forEach(ds -> {
      if (ds.getIdentifiers().contains(study.getId())) study.addSet(ds.getId());
    });
    return study;
  }

  private List<BaseStudy> addSets(List<BaseStudy> studies) {
    List<DocumentSet> documentSets = studySetService.getAll();
    studies.forEach(study ->
      documentSets.forEach(ds -> {
        if (ds.getIdentifiers().contains(study.getId())) study.addSet(ds.getId());
      }));
    return studies;
  }
}
