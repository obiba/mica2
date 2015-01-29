/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.obiba.mica.study.service.PublishedDatasetVariableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsPublishedDatasetVariableService extends AbstractPublishedDocumentService<DatasetVariable>
  implements PublishedDatasetVariableService {

  private static final Logger log = LoggerFactory.getLogger(EsPublishedDatasetVariableService.class);

  private static final String VARIABLE_PUBLISHED = "variable-published";
  private static final String VARIABLE_TYPE = "Variable";


  @Override
  public long getCountByStudyId(String studyId) {
    return executeCountQuery(buildStudyFilteredQuery(studyId));
  }

  @Inject
  private ObjectMapper objectMapper;

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
    BoolFilterBuilder boolFilter = FilterBuilders.boolFilter().must(FilterBuilders.termsFilter("studyIds", ids));
    return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), boolFilter);
  }

  private QueryBuilder buildStudyFilteredQuery(String id) {
    BoolFilterBuilder boolFilter = FilterBuilders.boolFilter().must(FilterBuilders.termFilter("studyIds", id));
    return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), boolFilter);
  }

  private long executeCountQuery(QueryBuilder queryBuilder) {
    SearchRequestBuilder requestBuilder = client.prepareSearch(getIndexName()) //
      .setTypes(getType()) //
      .setSearchType(SearchType.COUNT) //
      .setQuery(queryBuilder) //
      .setFrom(0) //
      .setSize(0);

    try {
      SearchResponse response = requestBuilder.execute().actionGet();
      return response.getHits().totalHits();
    } catch(IndexMissingException e) {
      log.error("Failed to execute variables count: {}", e);
      return 0;
    }
  }
}
