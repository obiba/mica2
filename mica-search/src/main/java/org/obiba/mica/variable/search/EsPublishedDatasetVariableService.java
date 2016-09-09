/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.variable.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsPublishedDatasetVariableService extends AbstractDocumentService<DatasetVariable>
  implements PublishedDatasetVariableService {

  private static final Logger log = LoggerFactory.getLogger(EsPublishedDatasetVariableService.class);

  private static final String VARIABLE_PUBLISHED = "variable-published";

  private static final String VARIABLE_TYPE = "Variable";

  private static final String STUDY_IDS_FIELD = "studyIds";

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Override
  public long getCountByStudyId(String studyId) {
    SearchResponse response = executeCountQuery(buildStudyFilteredQuery(studyId), null);

    if(response == null) {
      return 0;
    }

    return response.getHits().totalHits();
  }

  public Map<String, Long> getCountByStudyIds(List<String> studyIds) {
    SearchResponse response = executeCountQuery(buildStudiesFilteredQuery(studyIds),
      AggregationBuilders.terms(STUDY_IDS_FIELD).field(STUDY_IDS_FIELD).size(0));

    if(response == null) {
      return studyIds.stream().collect(Collectors.toMap(s -> s, s -> 0L));
    }

    Terms aggregation = response.getAggregations().get(STUDY_IDS_FIELD);
    return studyIds.stream().collect(Collectors.toMap(s -> s,
      s -> Optional.ofNullable(aggregation.getBucketByKey(s)).map(Terms.Bucket::getDocCount).orElse(0L)));
  }

  @Override
  protected DatasetVariable processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, DatasetVariable.class);
  }

  @Override
  protected String getIndexName() {
    return VARIABLE_PUBLISHED;
  }

  @Override
  protected String getType() {
    return VARIABLE_TYPE;
  }

  private QueryBuilder buildStudiesFilteredQuery(List<String> ids) {
    BoolQueryBuilder boolFilter = QueryBuilders.boolQuery().must(QueryBuilders.termsQuery(STUDY_IDS_FIELD, ids));
    return QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).must(boolFilter);
  }

  private QueryBuilder buildStudyFilteredQuery(String id) {
    BoolQueryBuilder boolFilter = QueryBuilders.boolQuery().must(QueryBuilders.termQuery(STUDY_IDS_FIELD, id));
    return QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).must(boolFilter);
  }

  private SearchResponse executeCountQuery(QueryBuilder queryBuilder, AbstractAggregationBuilder aggregationBuilder) {
    QueryBuilder accessFilter = filterByAccess();

    SearchRequestBuilder requestBuilder = client.prepareSearch(getIndexName()) //
      .setTypes(getType()) //
      .setSize(0) //
      .setQuery(
        accessFilter == null ? queryBuilder : QueryBuilders.boolQuery().must(queryBuilder).must(accessFilter)) //
      .setFrom(0) //
      .setSize(0);

    if(aggregationBuilder != null) {
      requestBuilder.addAggregation(aggregationBuilder);
    }

    try {
      return requestBuilder.execute().actionGet();
    } catch(IndexNotFoundException e) {
      return null; //ignoring
    }
  }

  @Nullable
  @Override
  protected QueryBuilder filterByAccess() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    ids.addAll(harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList()));

    if(ids.isEmpty()) return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"));

    BoolQueryBuilder orFilter = QueryBuilders.boolQuery();
    ids.stream().forEach(id -> orFilter.should(QueryBuilders.termQuery("datasetId", id)));

    return orFilter;
  }
}
