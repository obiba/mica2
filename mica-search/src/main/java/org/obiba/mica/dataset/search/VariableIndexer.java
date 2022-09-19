/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.event.DocumentSetDeletedEvent;
import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Indexer of variables, that reacts on dataset events.
 */
@Component
public class VariableIndexer {
  private static final Logger log = LoggerFactory.getLogger(VariableIndexer.class);

  private final Lock lock = new ReentrantLock();

  private final Indexer indexer;

  private final CollectedDatasetService collectedDatasetService;

  private final VariableSetService variableSetService;

  @Inject
  public VariableIndexer(Indexer indexer, CollectedDatasetService collectedDatasetService, VariableSetService variableSetService) {
    this.indexer = indexer;
    this.collectedDatasetService = collectedDatasetService;
    this.variableSetService = variableSetService;
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      clearDraftVariablesIndex();
      if (event.getVariables() != null) {
        deleteDatasetVariables(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, event.getPersistable());
        deleteDatasetVariables(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE, event.getPersistable());

        if (event.getPersistable() instanceof StudyDataset) {
          indexDatasetVariables(Indexer.PUBLISHED_VARIABLE_INDEX, collectedDatasetService.processVariablesForStudyDataset(
            (StudyDataset) event.getPersistable(), event.getVariables()));
        } else {
          indexDatasetVariables(Indexer.PUBLISHED_VARIABLE_INDEX, event.getVariables());
        }
      }

      if (event.hasHarmonizationVariables()) {
        indexDatasetVariables(Indexer.PUBLISHED_HVARIABLE_INDEX, event.getHarmonizationVariables());
      }
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      clearDraftVariablesIndex();
      deleteDatasetVariables(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, event.getPersistable());
      deleteDatasetVariables(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE, event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    lock.lock();
    try {
      log.debug("{} {} was deleted", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
      clearDraftVariablesIndex();
      deleteDatasetVariables(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, event.getPersistable());
      deleteDatasetVariables(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE, event.getPersistable());
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void documentSetUpdated(DocumentSetUpdatedEvent event) {
    if (!variableSetService.isForType(event.getPersistable())) return;
    lock.lock();
    try {
      List<DatasetVariable> toIndex = Lists.newArrayList();
      String id = event.getPersistable().getId();
      if (event.hasRemovedIdentifiers()) {
        List<DatasetVariable> toRemove = variableSetService.getVariables(event.getRemovedIdentifiers(), false);
        toRemove.forEach(var -> var.removeSet(id));
        toIndex.addAll(toRemove);
      }
      List<DatasetVariable> variables = variableSetService.getVariables(event.getPersistable(), false);
      variables.stream()
        .filter(var -> !var.containsSet(id))
        .forEach(var -> {
          var.addSet(id);
          toIndex.add(var);
        });
      indexer.indexAllIndexables(Indexer.PUBLISHED_VARIABLE_INDEX, toIndex);
    } finally {
      lock.unlock();
    }
  }

  @Async
  @Subscribe
  public void documentSetDeleted(DocumentSetDeletedEvent event) {
    if (!variableSetService.isForType(event.getPersistable())) return;
    lock.lock();
    try {
      DocumentSet documentSet = event.getPersistable();
      if (!documentSet.getIdentifiers().isEmpty()) {
        List<DatasetVariable> toIndex = Lists.newArrayList();
        List<DatasetVariable> toRemove = variableSetService.getVariables(event.getPersistable(), false);
        toRemove.forEach(var -> var.removeSet(documentSet.getId()));
        toIndex.addAll(toRemove);
        indexer.indexAllIndexables(Indexer.PUBLISHED_VARIABLE_INDEX, toIndex);
      }
    } finally {
      lock.unlock();
    }
  }

  //
  // Private methods
  //

  // legacy index, cleanup
  private void clearDraftVariablesIndex() {
    if (indexer.hasIndex(Indexer.DRAFT_VARIABLE_INDEX)) indexer.dropIndex(Indexer.DRAFT_VARIABLE_INDEX);
  }

  private void indexDatasetVariables(String indexName, Iterable<DatasetVariable> variables) {
    List<DocumentSet> documentSets = variableSetService.getAll();
    variables.forEach(variable ->
      documentSets.forEach(ds -> {
        if (ds.getIdentifiers().contains(variable.getId())) variable.addSet(ds.getId());
      }));
    indexer.indexAllIndexables(indexName, variables);
  }

  private void deleteDatasetVariables(String indexName, String type, Dataset dataset) {
    // remove variables that have this dataset as parent
    Map.Entry<String, String> termQuery = ImmutablePair.of("datasetId", dataset.getId());
    indexer.delete(indexName, type, termQuery);
  }
}
