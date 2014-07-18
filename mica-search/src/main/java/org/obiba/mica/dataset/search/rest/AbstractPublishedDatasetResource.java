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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset} from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDatasetResource.class);

  @Inject
  protected ApplicationContext applicationContext;

  @Inject
  protected Dtos dtos;

  @Inject
  protected Client client;

  @Inject
  protected ObjectMapper objectMapper;

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

  protected Mica.DatasetVariablesDto getDatasetVariableDtos(Class<T> clazz, @NotNull String datasetId, @Nullable String studyId, int from,
      int limit, @Nullable String sort, @Nullable String order) {
    BoolQueryBuilder query = QueryBuilders.boolQuery()
        .must(QueryBuilders.queryString(getDatasetVariableType(studyId).toString()).field("variableType"));

    if(studyId == null) {
      query.must(hasParentDatasetQuery(clazz, datasetId));
    } else {
      query.must(hasParentStudyQuery(studyId)).must(QueryBuilders.queryString(datasetId).field("datasetId"));
    }

    SearchRequestBuilder search = new SearchRequestBuilder(client) //
        .setIndices(studyId == null ? DatasetIndexer.PUBLISHED_DATASET_INDEX : StudyIndexer.PUBLISHED_STUDY_INDEX) //
        .setTypes(DatasetIndexer.VARIABLE_TYPE) //
        .setQuery(query) //
        .setFrom(from) //
        .setSize(limit);

    if(sort != null) {
      search.addSort(
          SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
    }

    log.info(search.toString());
    SearchResponse response = search.execute().actionGet();
    log.info(response.toString());

    Mica.DatasetVariablesDto.Builder builder = Mica.DatasetVariablesDto.newBuilder() //
        .setTotal(Long.valueOf(response.getHits().getTotalHits()).intValue()) //
        .setFrom(from) //
        .setLimit(limit);
    response.getHits().forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        builder.addVariables(dtos.asDto(objectMapper.readValue(inputStream, DatasetVariable.class)));
      } catch(IOException e) {
        log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      }
    });

    return builder.build();
  }

  protected abstract DatasetVariable.Type getDatasetVariableType(String studyId);

  protected QueryBuilder hasParentDatasetQuery(Class<T> clazz, @NotNull String datasetId) {
    return QueryBuilders.hasParentQuery(DatasetIndexer.DATASET_TYPE,
        QueryBuilders.boolQuery().must(QueryBuilders.idsQuery(DatasetIndexer.DATASET_TYPE).addIds(datasetId))
            .must(QueryBuilders.queryString(clazz.getSimpleName()).field("className")));
  }

  protected QueryBuilder hasParentStudyQuery(@NotNull String studyId) {
    return QueryBuilders.hasParentQuery(StudyIndexer.STUDY_TYPE,
        QueryBuilders.boolQuery().must(QueryBuilders.idsQuery(StudyIndexer.STUDY_TYPE).addIds(studyId)));
  }

}
