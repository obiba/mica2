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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.obiba.mica.dataset.service.VariableIndexer;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Indexer of variables, that reacts on dataset events.
 */
@Component
public class VariableIndexerImpl implements VariableIndexer {
  private static final Logger log = LoggerFactory.getLogger(VariableIndexerImpl.class);

  public static final String DRAFT_VARIABLE_INDEX = "variable-draft";

  public static final String PUBLISHED_VARIABLE_INDEX = "variable-published";

  public static final String VARIABLE_TYPE = DatasetVariable.MAPPING_NAME;

  public static final String HARMONIZED_VARIABLE_TYPE = DatasetVariable.HMAPPING_NAME;

  public static final String[] ANALYZED_FIELDS = {"name"};

  public static final String[] LOCALIZED_ANALYZED_FIELDS = {"label", "description"};

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Override
  public void onDatasetUpdated(Iterable<DatasetVariable> variables) {
    indexDatasetVariables(DRAFT_VARIABLE_INDEX, variables);
  }

  @Override
  public void onDatasetUpdated(Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizedVariables) {
    onDatasetUpdated(variables);
    indexHarmonizedVariables(DRAFT_VARIABLE_INDEX, harmonizedVariables);
  }

  @Override
  public void onDatasetPublished(Dataset dataset, Iterable<DatasetVariable> variables) {
    if (!dataset.isPublished()) {
      deleteDatasetVariables(PUBLISHED_VARIABLE_INDEX, dataset);
      return;
    }

    indexDatasetVariables(PUBLISHED_VARIABLE_INDEX, variables);
  }

  @Override
  public void onDatasetPublished(Dataset dataset, Iterable<DatasetVariable> variables,
    Map<String, List<DatasetVariable>> harmonizedVariables) {
    onDatasetPublished(dataset, variables);

    if (dataset.isPublished()) {
      indexHarmonizedVariables(PUBLISHED_VARIABLE_INDEX, harmonizedVariables);
    }
  }

  @Override
  public void onDatasetDeleted(Dataset dataset) {
    deleteDatasetVariables(DRAFT_VARIABLE_INDEX, dataset);
    deleteDatasetVariables(PUBLISHED_VARIABLE_INDEX, dataset);
  }

  @Override
  public void indexAll(Iterable<DatasetVariable> variables) {

  }

  @Override
  public void indexAll(Map<String, List<DatasetVariable>> harmonizedVariables) {

  }

  @Override
  public void dropIndex() {
    if(elasticSearchIndexer.hasIndex(DRAFT_VARIABLE_INDEX)) elasticSearchIndexer.dropIndex(DRAFT_VARIABLE_INDEX);
    if(elasticSearchIndexer.hasIndex(PUBLISHED_VARIABLE_INDEX)) elasticSearchIndexer.dropIndex(PUBLISHED_VARIABLE_INDEX);
  }

  //
  // Private methods
  //

  private void indexDatasetVariables(String indexName, Iterable<DatasetVariable> variables) {
    elasticSearchIndexer.indexAllIndexables(indexName, variables);
  }

  protected void indexHarmonizedVariables(String indexName, Map<String, List<DatasetVariable>> harmonizedVariables) {
      harmonizedVariables.keySet().forEach(
          parentId -> elasticSearchIndexer.indexAllIndexables(indexName, harmonizedVariables.get(parentId), parentId));
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
}
