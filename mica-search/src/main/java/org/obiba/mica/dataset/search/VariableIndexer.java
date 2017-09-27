/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.dataset.service.CollectionDatasetService;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

/**
 * Indexer of variables, that reacts on dataset events.
 */
@Component
public class VariableIndexer {
  private static final Logger log = LoggerFactory.getLogger(VariableIndexer.class);

  public static final String DRAFT_VARIABLE_INDEX = "variable-draft";

  public static final String PUBLISHED_VARIABLE_INDEX = "variable-published";

  public static final String VARIABLE_TYPE = DatasetVariable.MAPPING_NAME;

  public static final String HARMONIZED_VARIABLE_TYPE = DatasetVariable.HMAPPING_NAME;

  public static final String[] ANALYZED_FIELDS = { "name" };

  public static final String[] LOCALIZED_ANALYZED_FIELDS = { "label", "description" };

  @Inject
  private Indexer indexer;

  @Inject
  private CollectionDatasetService collectionDatasetService;

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    log.debug("{} {} was published", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    clearDraftVariablesIndex();
    if(event.getVariables() != null) {
      deleteDatasetVariables(PUBLISHED_VARIABLE_INDEX, event.getPersistable());

      if (event.getPersistable() instanceof StudyDataset) {
        indexDatasetVariables(PUBLISHED_VARIABLE_INDEX, collectionDatasetService.processVariablesForStudyDataset(
          (StudyDataset) event.getPersistable(), event.getVariables()));
      } else {
        indexDatasetVariables(PUBLISHED_VARIABLE_INDEX, event.getVariables());
      }
    }

    if(event.hasHarmonizationVariables())
      indexHarmonizedVariables(PUBLISHED_VARIABLE_INDEX, event.getHarmonizationVariables());
  }

  @Async
  @Subscribe
  public void datasetUnpublished(DatasetUnpublishedEvent event) {
    log.debug("{} {} was unpublished", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    clearDraftVariablesIndex();
    deleteDatasetVariables(PUBLISHED_VARIABLE_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    log.debug("{} {} was deleted", event.getPersistable().getClass().getSimpleName(), event.getPersistable());
    clearDraftVariablesIndex();
    deleteDatasetVariables(PUBLISHED_VARIABLE_INDEX, event.getPersistable());
  }

  //
  // Private methods
  //

  // legacy index, cleanup
  private void clearDraftVariablesIndex() {
    if(indexer.hasIndex(VariableIndexer.DRAFT_VARIABLE_INDEX)) indexer.dropIndex(VariableIndexer.DRAFT_VARIABLE_INDEX);
  }

  private void indexDatasetVariables(String indexName, Iterable<DatasetVariable> variables) {
    indexer.indexAllIndexables(indexName, variables);
  }

  protected void indexHarmonizedVariables(String indexName, Map<String, List<DatasetVariable>> harmonizationVariables) {
    harmonizationVariables.keySet().forEach(
      parentId -> indexer.indexAllIndexables(indexName, harmonizationVariables.get(parentId), parentId));
  }

  private void deleteDatasetVariables(String indexName, Dataset dataset) {
    // remove variables that have this dataset as parent
    Map.Entry<String, String> termQuery = ImmutablePair.of("datasetId", dataset.getId());
    indexer.delete(indexName, HARMONIZED_VARIABLE_TYPE, termQuery);
    indexer.delete(indexName, VARIABLE_TYPE, termQuery);
  }
}
