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

import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.domain.Indexable;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.obiba.mica.study.StudyService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.search.StudyIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;

public abstract class DatasetIndexer<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(DatasetIndexer.class);

  public static final String DRAFT_DATASET_INDEX = "dataset-draft";

  public static final String PUBLISHED_DATASET_INDEX = "dataset-published";

  public static final String VARIABLE_TYPE = DatasetVariable.MAPPING_NAME;

  @Inject
  protected StudyService studyService;

  @Inject
  protected ElasticSearchIndexer elasticSearchIndexer;

  protected void reIndex(T dataset) {
    deleteFromDatasetIndices(dataset);
    elasticSearchIndexer.index(DRAFT_DATASET_INDEX, (Indexable) dataset);
    Iterable<DatasetVariable> variables = getVariables(dataset);
    indexDatasetVariables(DRAFT_DATASET_INDEX, variables, dataset);
    if(dataset.isPublished()) {
      elasticSearchIndexer.index(PUBLISHED_DATASET_INDEX, (Indexable) dataset);
      indexDatasetVariables(PUBLISHED_DATASET_INDEX, variables, dataset);
    }
  }

  protected void reIndex(T dataset, String studyId) {
    deleteFromStudyIndices(dataset, studyId);
    indexStudyVariables(StudyIndexer.DRAFT_STUDY_INDEX, dataset,
        studyService.findDraftStudy(studyId));
    if (studyService.isPublished(studyId)) {
      indexStudyVariables(StudyIndexer.PUBLISHED_STUDY_INDEX, dataset,
          studyService.findPublishedStudy(studyId));
    }
  }

  protected void deleteFromDatasetIndices(T dataset) {
    deleteFromDatasetIndex(DRAFT_DATASET_INDEX, dataset);
    deleteFromDatasetIndex(PUBLISHED_DATASET_INDEX, dataset);
  }

  private void deleteFromDatasetIndex(String indexName, T dataset) {
    // remove variables that have this dataset as parent
    QueryBuilder datasetChildrenQuery = QueryBuilders
        .hasParentQuery(Dataset.MAPPING_NAME, QueryBuilders.idsQuery(Dataset.MAPPING_NAME).addIds(dataset.getId()));

    elasticSearchIndexer.delete(indexName, VARIABLE_TYPE, datasetChildrenQuery);
    elasticSearchIndexer.delete(indexName, (Indexable) dataset);
  }

  protected void deleteFromStudyIndices(T dataset, String studyId) {
    deleteFromStudyIndex(StudyIndexer.DRAFT_STUDY_INDEX, dataset, studyId);
    deleteFromStudyIndex(StudyIndexer.PUBLISHED_STUDY_INDEX, dataset, studyId);
  }

  private void deleteFromStudyIndex(String indexName, T dataset, String studyId) {
    // remove variables that have the study as parent and belongs to the dataset
    QueryBuilder studyChildrenQuery = QueryBuilders.hasParentQuery(Study.class.getSimpleName(),
        QueryBuilders.idsQuery(Study.class.getSimpleName()).addIds(studyId));
    QueryBuilder query = QueryBuilders.boolQuery().must(studyChildrenQuery)
        .must(QueryBuilders.termQuery("datasetId", dataset.getId()));

    elasticSearchIndexer.delete(indexName, VARIABLE_TYPE, query);
  }

  protected void reIndexAll() {
    reIndexAll(DRAFT_DATASET_INDEX, findAllDatasets());
    reIndexAll(PUBLISHED_DATASET_INDEX, findAllPublishedDatasets());
  }

  private void reIndexAll(String indexName, Iterable<T> datasets) {
    //if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    // TODO delete dataset by their className and delete their children as-well
    elasticSearchIndexer.indexAllIndexables(indexName, datasets);
    datasets.forEach(dataset -> indexDatasetVariables(indexName, getVariables(dataset), dataset));
  }

  private void indexDatasetVariables(String indexName, Iterable<DatasetVariable> variables, Indexable parent) {
    elasticSearchIndexer.indexAllIndexables(indexName, variables, parent);
  }

  protected void indexStudyVariables(String indexName, T dataset, Study study) {
    try {
      indexDatasetVariables(indexName, getVariables(dataset, study), asIndexable(study));
    } catch(Exception e) {
      log.error("Unable to index variables of dataset {} in {}", dataset, indexName, e);
    }
  }

  public Indexable asIndexable(final Persistable<String> persistable) {
    return ElasticSearchIndexer.asIndexable(persistable);
  }

  protected abstract Iterable<DatasetVariable> getVariables(T dataset);

  protected abstract Iterable<DatasetVariable> getVariables(T dataset, Study study);

  protected abstract Iterable<T> findAllDatasets();

  protected abstract Iterable<T> findAllPublishedDatasets();
}
