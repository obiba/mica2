/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.json.JSONException;
import org.obiba.mica.study.StudyService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.search.StudyIndexer;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Path("/studies/_search")
public class PublishedStudiesSearchResource {

  private static final String STUDY_FACETS_YML = "study-facets.yml";

  private static final Logger log = LoggerFactory.getLogger(PublishedStudiesSearchResource.class);

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private Client client;

  @Inject
  private AggregationYamlParser aggregationYamlParser;

  @Inject
  private StudyService studyService;

  @GET
  @Timed
  public QueryResultDto list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("detailed") @DefaultValue("false") boolean detailed)
      throws JSONException, IOException {

    SearchRequestBuilder requestBuilder = client.prepareSearch(StudyIndexer.PUBLISHED_STUDY_INDEX) //
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH) //
        .setQuery(QueryBuilders.matchAllQuery()) //
        .setFrom(from) //
        .setSize(size);
    aggregationYamlParser.getAggregations(STUDY_FACETS_YML).forEach(requestBuilder::addAggregation);

    log.info("Request: {}", requestBuilder.toString());
    SearchResponse response = requestBuilder.execute().actionGet();
    log.info("Response: {}", response.toString());

    QueryResultDto.Builder builder = QueryResultDto.newBuilder().setTotalHits((int) response.getHits().getTotalHits());

    for(SearchHit hit : response.getHits()) {
      try(InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes())) {
        builder.addSummaries(dtos.asDto(studyService.findStateById(hit.getId())));
        if(detailed) {
          builder.addStudies(dtos.asDto(objectMapper.readValue(inputStream, Study.class)));
        }
      }
    }

    response.getAggregations() //
        .forEach(aggregation -> {
          MicaSearch.AggregationResultDto.Builder aggResultBuilder = MicaSearch.AggregationResultDto.newBuilder();
          aggResultBuilder.setAggregation(aggregation.getName());
          ((Terms) aggregation).getBuckets().forEach(bucket -> aggResultBuilder.addTermsAggregations(
              MicaSearch.AggregationResultDto.TermsAggregationResultDto.newBuilder().setKey(bucket.getKey())
                  .setCount((int) bucket.getDocCount())));
          builder.addAggs(aggResultBuilder.build());
        });

    return builder.build();
  }

}
