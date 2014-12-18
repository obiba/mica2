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
import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.search.AbstractDatasetIndexer;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset}s from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetsResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDatasetsResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private Client client;

  @Inject
  private ObjectMapper objectMapper;

  protected Mica.DatasetsDto getDatasetDtos(Class<T> clazz, int from, int limit, @Nullable String sort,
      @Nullable String order, @Nullable String studyId, @Nullable String queryString) {
    QueryBuilder query = QueryBuilders.queryString(clazz.getSimpleName()).field("className");
    if (queryString != null) {
      query = QueryBuilders.boolQuery().must(query).must(QueryBuilders.queryString(queryString));
    }
    FilterBuilder filter = null;
    if(studyId != null) {
      filter = FilterBuilders.termFilter(getStudyIdField(), studyId);
    }

    SearchRequestBuilder search = client.prepareSearch() //
        .setIndices(AbstractDatasetIndexer.PUBLISHED_DATASET_INDEX) //
        .setTypes(AbstractDatasetIndexer.DATASET_TYPE) //
        .setQuery(query) //
        .setPostFilter(filter) //
        .setFrom(from) //
        .setSize(limit);

    if(sort != null) {
      search.addSort(SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
    }

    log.debug("Request: {}", search.toString());
    SearchResponse response = search.execute().actionGet();

    Mica.DatasetsDto.Builder builder = Mica.DatasetsDto.newBuilder() //
        .setTotal(Long.valueOf(response.getHits().getTotalHits()).intValue()) //
        .setFrom(from) //
        .setLimit(limit);
    response.getHits().forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        builder.addDatasets(dtos.asDto(objectMapper.readValue(inputStream, clazz)));
      } catch(IOException e) {
        log.error("Failed retrieving {}", clazz.getSimpleName(), e);
      }
    });

    return builder.build();
  }

  protected abstract String getStudyIdField();

}
