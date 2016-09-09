/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.search.NetworkIndexer;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.NetworkAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.NetworkTaxonomyMetaDataProvider;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.obiba.mica.search.CountStatsDtoBuilders.NetworkCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.NetworkResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class NetworkQuery extends AbstractDocumentQuery {

  private static final Logger log = LoggerFactory.getLogger(NetworkQuery.class);

  public static final String JOIN_FIELD = "studyIds";

  @Inject
  Dtos dtos;

  @Inject
  PublishedNetworkService publishedNetworkService;

  @Inject
  private NetworkAggregationMetaDataProvider networkAggregationMetaDataProvider;

  @Inject
  private NetworkTaxonomyMetaDataProvider networkTaxonomyMetaDataProvider;

  @Override
  public String getSearchIndex() {
    return NetworkIndexer.PUBLISHED_NETWORK_INDEX;
  }

  @Override
  public String getSearchType() {
    return NetworkIndexer.NETWORK_TYPE;
  }

  @Override
  public QueryBuilder getAccessFilter() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = publishedNetworkService.getNetworkService().findPublishedStates().stream()
      .map(NetworkState::getId).filter(s -> subjectAclService.isAccessible("/network", s)).collect(Collectors.toList());
    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(NetworkIndexer.LOCALIZED_ANALYZED_FIELDS);
  }

  @Nullable
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = getAggregationsProperties(filter, taxonomyService.getNetworkTaxonomy());
    if(!properties.containsKey(JOIN_FIELD)) properties.put(JOIN_FIELD,"");
    return properties;
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope, CountStatsData counts) {
    NetworkResultDto.Builder resBuilder = NetworkResultDto.newBuilder();
    NetworkCountStatsBuilder networkCountStatsBuilder = counts == null
      ? null
      : NetworkCountStatsBuilder.newBuilder(counts);

    Consumer<Network> addDto = getNetworkConsumer(scope, resBuilder, networkCountStatsBuilder);
    List<Network> networks = publishedNetworkService
      .findByIds(Stream.of(hits.hits()).map(h -> h.getId()).collect(Collectors.toList()));
    networks.forEach(addDto::accept);
    builder.setExtension(NetworkResultDto.result, resBuilder.build());
  }

  private Consumer<Network> getNetworkConsumer(Scope scope, NetworkResultDto.Builder resBuilder,
    NetworkCountStatsBuilder networkCountStatsBuilder) {

    return scope == Scope.DETAIL ? (network) -> {
      Mica.NetworkDto.Builder networkBuilder = dtos.asDtoBuilder(network);
      if(mode == Mode.LIST) {
        networkBuilder.clearStudySummaries();
        networkBuilder.clearContacts();
        networkBuilder.clearInvestigators();
      }
      if(networkCountStatsBuilder != null) {
        networkBuilder.setExtension(MicaSearch.CountStatsDto.networkCountStats, networkCountStatsBuilder.build(network))
          .build();
      }
      resBuilder.addNetworks(networkBuilder.build());
    } : (network) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(network).build());
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(networkAggregationMetaDataProvider, networkTaxonomyMetaDataProvider);
  }

  public Map<String, List<String>> getStudyCountsByNetwork() {
    SearchRequestBuilder requestBuilder = client.prepareSearch(getSearchIndex()) //
      .setTypes(getSearchType()) //
      .setSearchType(SearchType.COUNT) //
      .setQuery(hasQueryBuilder() ? getQueryBuilder() : QueryBuilders.matchAllQuery()) //
      .setNoFields();

    Properties props = new Properties();
    props.setProperty("id", "");
    Properties subProps = new Properties();
    subProps.setProperty(JOIN_FIELD, "");
    Map<String, Properties> subAggregations = Maps.newHashMap();
    subAggregations.put("id", subProps);
    try {
      aggregationYamlParser.getAggregations(props, subAggregations).forEach(requestBuilder::addAggregation);
    } catch(IOException e) {
      log.error("Failed to add Study By Network aggregations");
      return Maps.newHashMap();
    }

    Map<String, List<String>> map = Maps.newHashMap();
    try {
      SearchResponse response = requestBuilder.execute().actionGet();

      response.getAggregations().forEach(
        aggregation -> ((Terms) aggregation).getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
          .forEach(bucket -> map.put(bucket.getKeyAsString(), getStudyCounts(bucket.getAggregations()))));
    } catch(IndexNotFoundException e) {
      // ignore
    }

    return map;
  }

  private List<String> getStudyCounts(Aggregations aggregations) {
    List<String> list = Lists.newArrayList();
    aggregations.forEach(
      aggregation -> ((Terms) aggregation).getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
        .forEach(bucket -> list.add(bucket.getKeyAsString())));

    return list;
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

}
