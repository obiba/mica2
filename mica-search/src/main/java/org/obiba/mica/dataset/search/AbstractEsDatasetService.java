/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.dataset.service.EsDatasetService;
import org.obiba.mica.search.AbstractDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEsDatasetService<T> extends AbstractDocumentService<T> implements EsDatasetService {
  private static final Logger log = LoggerFactory.getLogger(AbstractEsDatasetService.class);

  @Override
  public long getStudiesWithVariablesCount() {
    BoolQueryBuilder builder = QueryBuilders.boolQuery()
      .should(QueryBuilders.existsQuery("studyTable.studyId"));

    SearchRequestBuilder requestBuilder = searcher.prepareSearch(getIndexName()) //
      .setTypes(getType()) //
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
      .setQuery(builder)
      .setFrom(0) //
      .setSize(0);

    requestBuilder.addAggregation(AggregationBuilders.terms("studyTable-studyId").field("studyTable.studyId"));

    try {
      log.debug("Request /{}/{}: {}", getIndexName(), getType(), requestBuilder);
      SearchResponse response = requestBuilder.execute().actionGet();
      log.debug("Response /{}/{}: {}", getIndexName(), getType(), response);

      return response.getAggregations().asList().stream().flatMap(a -> ((Terms) a).getBuckets().stream())
        .map(a -> a.getKey().toString()).distinct().collect(Collectors.toList()).size();
    } catch(IndexNotFoundException e) {
      log.warn("Count of Studies With Variables failed: {}", e);
      return 0;
    }

  }


}
