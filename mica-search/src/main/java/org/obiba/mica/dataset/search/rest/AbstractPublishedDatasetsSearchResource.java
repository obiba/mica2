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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset}s from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetsSearchResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDatasetsSearchResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private Client client;

  @Inject
  private ObjectMapper objectMapper;

  protected List<Mica.DatasetDto> getDatasetDtos(Class<T> clazz, int from, int limit, @Nullable String sort,
      @Nullable String order, @Nullable String studyId) {
    QueryBuilder query = QueryBuilders.queryString(clazz.getSimpleName()).field("className");
    if(studyId != null) {
      query = QueryBuilders.boolQuery().must(query)
          .must(QueryBuilders.queryString(studyId).field(getStudyIdField()));
    }

    SearchRequestBuilder search = new SearchRequestBuilder(client) //
        .setIndices(DatasetIndexer.PUBLISHED_DATASET_INDEX) //
        .setTypes(DatasetIndexer.DATASET_TYPE) //
        .setQuery(query) //
        .setFrom(from) //
        .setSize(limit);

    if(sort != null) {
      search.addSort(SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
    }

    log.info(search.toString());
    SearchResponse response = search.execute().actionGet();
    log.info(response.toString());

    ImmutableList.Builder<Mica.DatasetDto> builder = ImmutableList.builder();
    response.getHits().forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        builder.add(dtos.asDto(objectMapper.readValue(inputStream, clazz)));
      } catch(IOException e) {
        log.error("Failed retrieving {}", clazz.getSimpleName(), e);
      }
    });

    return builder.build();
  }

  protected abstract String getStudyIdField();

}
