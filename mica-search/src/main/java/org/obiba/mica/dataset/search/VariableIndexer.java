/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUpdatedEvent;
import org.obiba.mica.dataset.event.IndexHarmonizationDatasetsEvent;
import org.obiba.mica.dataset.event.IndexStudyDatasetsEvent;
import org.obiba.mica.domain.StudyTable;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.service.HarmonizationDatasetService;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.study.NoSuchStudyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
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

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void datasetUpdated(DatasetUpdatedEvent event) {
    if(event.isStudyDataset()) {
      indexDatasetVariables(DRAFT_VARIABLE_INDEX, getVariables((StudyDataset) event.getPersistable()));
    } else {
      HarmonizationDataset ds = (HarmonizationDataset) event.getPersistable();
      indexDatasetVariables(DRAFT_VARIABLE_INDEX, getVariables(ds));
      indexHarmonizedVariables(DRAFT_VARIABLE_INDEX, ds);
    }
  }

  @Async
  @Subscribe
  public void datasetPublished(DatasetPublishedEvent event) {
    if(event.isStudyDataset()) {
      indexDatasetVariables(PUBLISHED_VARIABLE_INDEX, getVariables((StudyDataset) event.getPersistable()));
    } else {
      HarmonizationDataset ds = (HarmonizationDataset) event.getPersistable();
      indexDatasetVariables(PUBLISHED_VARIABLE_INDEX, getVariables(ds));
      indexHarmonizedVariables(PUBLISHED_VARIABLE_INDEX, ds);
    }
  }

  @Async
  @Subscribe
  public void datasetDeleted(DatasetDeletedEvent event) {
    deleteDatasetVariables(DRAFT_VARIABLE_INDEX, event.getPersistable());
    deleteDatasetVariables(PUBLISHED_VARIABLE_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void indexAll(IndexHarmonizationDatasetsEvent event) {

  }

  @Async
  @Subscribe
  public void indexAll(IndexStudyDatasetsEvent event) {

  }

  //
  // Private methods
  //

  private void indexDatasetVariables(String indexName, Iterable<DatasetVariable> variables) {
    elasticSearchIndexer.indexAllIndexables(indexName, variables);
  }

  protected void indexHarmonizedVariables(String indexName, HarmonizationDataset dataset) {
    try {
      Map<String, List<DatasetVariable>> harmonizedVariables = Maps.newHashMap();

      for(StudyTable studyTable : dataset.getStudyTables()) {
        try {
          populateHarmonizedVariablesMap(harmonizedVariables, getVariables(dataset, studyTable.getStudyId()));
        } catch(NoSuchValueTableException e) {
          // ignore (case the study does not implement this harmonization dataset
        }
      }

      harmonizedVariables.keySet().forEach(
          parentId -> elasticSearchIndexer.indexAllIndexables(indexName, harmonizedVariables.get(parentId), parentId));

    } catch(Exception e) {
      log.error("Unable to index variables of dataset {} in {}", dataset, indexName, e);
    }
  }

  private void populateHarmonizedVariablesMap(Map<String, List<DatasetVariable>> map,
      Iterable<DatasetVariable> variables) {
    for(DatasetVariable variable : variables) {
      if(!map.containsKey(variable.getParentId())) {
        map.put(variable.getParentId(), new ArrayList<DatasetVariable>());
      }
      map.get(variable.getParentId()).add(variable);
    }
  }

  private void deleteDatasetVariables(String indexName, Dataset dataset) {
    // remove variables that have this dataset as parent
    QueryBuilder query = QueryBuilders.termQuery("datasetId", dataset.getId());
    checkDeleteResponse(indexName, query, elasticSearchIndexer.delete(indexName, VARIABLE_TYPE, query));
    checkDeleteResponse(indexName, query, elasticSearchIndexer.delete(indexName, HARMONIZED_VARIABLE_TYPE, query));
  }

  private void checkDeleteResponse(String indexName, QueryBuilder query, DeleteByQueryResponse response) {
    if(response.status().getStatus() >= RestStatus.BAD_REQUEST.getStatus()) {
      log.error("Delete variables from {} failed ({}): {}", indexName, response.status(), query.toString());
    }
  }

  /**
   * Get the dataschema variables of the {@link org.obiba.mica.dataset.domain.HarmonizationDataset}.
   *
   * @param dataset
   * @return
   */
  private Iterable<DatasetVariable> getVariables(HarmonizationDataset dataset) throws NoSuchValueTableException {
    return harmonizationDatasetService.getDatasetVariables(dataset);
  }

  /**
   * Get the harmonized variables of the {@link org.obiba.mica.dataset.domain.HarmonizationDataset}.
   *
   * @param dataset
   * @param studyId
   * @return
   */
  protected Iterable<DatasetVariable> getVariables(HarmonizationDataset dataset, String studyId)
      throws NoSuchStudyException, NoSuchValueTableException {
    return harmonizationDatasetService.getDatasetVariables(dataset, studyId);
  }

  /**
   * Get the study variables of the {@link org.obiba.mica.dataset.domain.StudyDataset}.
   *
   * @param dataset
   * @return
   */
  private Iterable<DatasetVariable> getVariables(StudyDataset dataset) {
    return studyDatasetService.getDatasetVariables(dataset);
  }

}
