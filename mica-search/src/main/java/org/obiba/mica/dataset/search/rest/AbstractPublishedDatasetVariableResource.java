/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.search.rest.variable.PublishedDatasetVariableResource;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.DatasetVariable} from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetVariableResource<T extends Dataset>
    extends AbstractPublishedDatasetResource<T> implements DatasetVariableResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedDatasetVariableResource.class);

  /**
   * Look for a variable of a dataset in the published dataset index or the published study index depending on
   * whether a study ID is provided for resolving variable's parent.
   *
   * @param clazz
   * @param datasetId
   * @param variableName
   * @param studyId
   * @return
   * @throws NoSuchVariableException
   */
  protected DatasetVariable getDatasetVariable(Class<T> clazz, @NotNull String datasetId, @NotNull String variableName,
      @Nullable String studyId) throws NoSuchVariableException {
    BoolQueryBuilder query = QueryBuilders.boolQuery()
        .must(QueryBuilders.queryString(getDatasetVariableType().toString()).field("variableType"))
        .must(QueryBuilders.queryString(variableName).field("name"));

    if(studyId == null) {
      query.must(hasParentDatasetQuery(clazz, datasetId));
    } else {
      query.must(hasParentStudyQuery(studyId)).must(QueryBuilders.queryString(datasetId).field("datasetId"));
    }

    SearchRequestBuilder search = new SearchRequestBuilder(client) //
        .setIndices(studyId == null ? DatasetIndexer.PUBLISHED_DATASET_INDEX : StudyIndexer.PUBLISHED_STUDY_INDEX) //
        .setTypes(DatasetIndexer.VARIABLE_TYPE) //
        .setQuery(query);

    log.info(search.toString());
    SearchResponse response = search.execute().actionGet();
    log.info(response.toString());

    if(response.getHits().totalHits() == 0) throw new NoSuchVariableException(variableName);

    InputStream inputStream = new ByteArrayInputStream(response.getHits().hits()[0].getSourceAsString().getBytes());
    try {
      return objectMapper.readValue(inputStream, DatasetVariable.class);
    } catch(IOException e) {
      log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      throw new NoSuchVariableException(variableName);
    }
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(Class<T> clazz, @NotNull String datasetId,
      @NotNull String variableName) {
    return getDatasetVariableDto(clazz, datasetId, variableName, null);
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(Class<T> clazz, @NotNull String datasetId,
      @NotNull String variableName, @Nullable String studyId) {
    return dtos.asDto(getDatasetVariable(clazz, datasetId, variableName, studyId));
  }

}
