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
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.json.JSONException;
import org.obiba.mica.search.ElasticSearchService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import static org.obiba.mica.web.model.Search.QueryResultDto;

@Component
@Scope("request")
@Path("/search/published")
public class PublishedStudiesIndexResource {

  private static final Logger log = LoggerFactory.getLogger(PublishedStudiesIndexResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private ElasticSearchService elasticSearchService;

  /**
   * TODO: this method should be renamed to _search and receive a queryDto
   *
   * @param from
   * @param size
   * @return
   * @throws JSONException
   * @throws IOException
   */
  @GET
  @Path("/studies")
  public QueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("size") @DefaultValue("10") int size) throws JSONException, IOException {

    SearchResponse response = elasticSearchService.getClient() //
        .prepareSearch("study-published") //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(QueryBuilders.matchAllQuery()) //
        .addAggregation(AggregationBuilders.terms("access").field("access")) //
        .setFrom(from) //
        .setSize(size) //
        .execute() //
        .actionGet();


    QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int)response.getHits().getTotalHits());

    for (SearchHit hit : response.getHits()) {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      builder.addStudies(dtos.asDto(objectMapper.readValue(inputStream, Study.class)));
    }

    for (Aggregation aggregation : response.getAggregations()) {
      StringTerms terms = (StringTerms)aggregation;

      for (Bucket bucket : terms.getBuckets()) {
        builder.addAggs(
            Search.TermsAggregateDto.newBuilder().setKey(bucket.getKey()).setCount((int) bucket.getDocCount()));
      }
    }

    return builder.build();
  }

}
