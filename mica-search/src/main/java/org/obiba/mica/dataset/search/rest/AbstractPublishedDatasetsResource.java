/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
import java.util.stream.Collectors;

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
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
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

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  protected Mica.DatasetsDto getDatasetDtos(Class<T> clazz, int from, int limit, @Nullable String sort,
    @Nullable String order, @Nullable String studyId, @Nullable String queryString) {
    QueryBuilder query = QueryBuilders.queryStringQuery(clazz.getSimpleName()).field("className");
    if(queryString != null) {
      query = QueryBuilders.boolQuery().must(query).must(QueryBuilders.queryStringQuery(queryString));
    }

    QueryBuilder postFilter = getPostFilter(clazz, studyId);

    SearchRequestBuilder search = client.prepareSearch() //
      .setIndices(DatasetIndexer.PUBLISHED_DATASET_INDEX) //
      .setTypes(DatasetIndexer.DATASET_TYPE) //
      .setQuery(postFilter == null ? query : QueryBuilders.boolQuery().must(query).must(postFilter)) //
      .setFrom(from) //
      .setSize(limit);

    if(sort != null) {
      search.addSort(
        SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
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

  @Nullable
  private QueryBuilder getPostFilter(Class<T> clazz, @Nullable String studyId) {
    QueryBuilder filter = filterByAccessibility(clazz);

    if(studyId != null) {
      QueryBuilder filterByStudy = QueryBuilders.termQuery(getStudyIdField(), studyId);
      filter = filter == null ? filterByStudy : QueryBuilders.boolQuery().must(filter).must(filterByStudy);
    }

    return filter;
  }

  protected QueryBuilder filterByAccessibility(Class<T> clazz) {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids;
    if("StudyDataset".equals(clazz.getSimpleName()))
      ids = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
        .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    else ids = harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList());

    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.termQuery("id", ids);
  }

}
