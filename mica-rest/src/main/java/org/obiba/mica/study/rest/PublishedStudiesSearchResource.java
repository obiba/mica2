/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.json.JSONException;
import org.obiba.mica.search.ElasticSearchService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.obiba.mica.web.model.Search.QueryResultDto;

@Path("/studies/_search")
public class PublishedStudiesSearchResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedStudiesSearchResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private ElasticSearchService elasticSearchService;

  @GET
  @Timed
  public QueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("size") @DefaultValue("10") int size) throws JSONException, IOException {

    SearchResponse response = elasticSearchService.getClient() //
        .prepareSearch(StudyIndexer.PUBLISHED_STUDY_INDEX) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(QueryBuilders.matchAllQuery()) //
        .addAggregation(AggregationBuilders.terms("access").field("access")) //
        .setFrom(from) //
        .setSize(size) //
        .execute() //
        .actionGet();

    QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int) response.getHits().getTotalHits());

    for(SearchHit hit : response.getHits()) {
      try(InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes())) {
        builder.addStudies(dtos.asDto(objectMapper.readValue(inputStream, Study.class)));
      }
    }
    response.getAggregations() //
        .forEach(aggregation -> ((Terms) aggregation).getBuckets() //
            .forEach(bucket -> builder.addAggs(
                Search.TermsAggregateDto.newBuilder().setKey(bucket.getKey()).setCount((int) bucket.getDocCount()))));
    return builder.build();
  }

}
