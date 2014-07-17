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
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset} from the published dataset index.
 *
 * @param <T>
 */
public abstract class PublishedDatasetSearchResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedDatasetSearchResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private Client client;

  @Inject
  private ObjectMapper objectMapper;

  protected T getDataset(Class<T> clazz, @NotNull String datasetId) throws NoSuchDatasetException {
    QueryBuilder query = QueryBuilders.queryString(clazz.getSimpleName()).field("className");
    query = QueryBuilders.boolQuery().must(query)
        .must(QueryBuilders.idsQuery(DatasetIndexer.DATASET_TYPE).addIds(datasetId));

    SearchRequestBuilder search = new SearchRequestBuilder(client) //
        .setIndices(DatasetIndexer.PUBLISHED_DATASET_INDEX) //
        .setTypes(DatasetIndexer.DATASET_TYPE) //
        .setQuery(query);

    log.info(search.toString());
    SearchResponse response = search.execute().actionGet();
    log.info(response.toString());

    if(response.getHits().totalHits() == 0) throw NoSuchDatasetException.withId(datasetId);

    InputStream inputStream = new ByteArrayInputStream(response.getHits().hits()[0].getSourceAsString().getBytes());
    try {
      return objectMapper.readValue(inputStream, clazz);
    } catch(IOException e) {
      log.error("Failed retrieving {}", clazz.getSimpleName(), e);
      throw NoSuchDatasetException.withId(datasetId);
    }
  }

  protected Mica.DatasetDto getDatasetDto(Class<T> clazz, @NotNull String datasetId) throws NoSuchDatasetException {
    return dtos.asDto(getDataset(clazz, datasetId));
  }

  public List<Mica.DatasetVariableDto> getDatasetVariableDtos(Class<T> clazz, @NotNull String datasetId) {
    QueryBuilder query = QueryBuilders.boolQuery().must(hasParentDatasetQuery(clazz, datasetId))
        .must(QueryBuilders.queryString(getDatasetVariableType().toString()).field("variableType"));

    SearchRequestBuilder search = new SearchRequestBuilder(client) //
        .setIndices(DatasetIndexer.PUBLISHED_DATASET_INDEX) //
        .setTypes(DatasetIndexer.VARIABLE_TYPE) //
        .setQuery(query);

    log.info(search.toString());
    SearchResponse response = search.execute().actionGet();
    log.info(response.toString());

    ImmutableList.Builder<Mica.DatasetVariableDto> builder = ImmutableList.builder();
    response.getHits().forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        builder.add(dtos.asDto(objectMapper.readValue(inputStream, DatasetVariable.class)));
      } catch(IOException e) {
        log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      }
    });

    return builder.build();
  }

  public DatasetVariable getDatasetVariable(Class<T> clazz, @NotNull String datasetId, @NotNull String variableName)
      throws NoSuchVariableException {
    QueryBuilder query = QueryBuilders.boolQuery().must(hasParentDatasetQuery(clazz, datasetId))
        .must(QueryBuilders.queryString(getDatasetVariableType().toString()).field("variableType"))
        .must(QueryBuilders.queryString(variableName).field("name"));

    SearchRequestBuilder search = new SearchRequestBuilder(client) //
        .setIndices(DatasetIndexer.PUBLISHED_DATASET_INDEX) //
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

  public Mica.DatasetVariableDto getDatasetVariableDto(Class<T> clazz, @NotNull String datasetId,
      @NotNull String variableName) {
    return dtos.asDto(getDatasetVariable(clazz, datasetId, variableName));
  }

  protected abstract DatasetVariable.Type getDatasetVariableType();

  private QueryBuilder hasParentDatasetQuery(Class<T> clazz, @NotNull String datasetId) {
    return QueryBuilders.hasParentQuery(DatasetIndexer.DATASET_TYPE,
        QueryBuilders.boolQuery().must(QueryBuilders.idsQuery(DatasetIndexer.DATASET_TYPE).addIds(datasetId))
            .must(QueryBuilders.queryString(clazz.getSimpleName()).field("className")));
  }

}
