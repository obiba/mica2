/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.aggregations.NetworkAggregationMetaDataProvider;
import org.obiba.mica.search.aggregations.NetworkTaxonomyMetaDataProvider;
import org.obiba.mica.search.aggregations.NetworksSetsAggregationMetaDataProvider;
import org.obiba.mica.spi.search.CountStatsData;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.QueryMode;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.AggregationHelper;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

  private final NetworkAggregationMetaDataProvider networkAggregationMetaDataProvider;

  private final NetworkTaxonomyMetaDataProvider networkTaxonomyMetaDataProvider;

  private final NetworksSetsAggregationMetaDataProvider networksSetsAggregationMetaDataProvider;

  @Inject
  public NetworkQuery(NetworkAggregationMetaDataProvider networkAggregationMetaDataProvider,
                      NetworkTaxonomyMetaDataProvider networkTaxonomyMetaDataProvider,
                      NetworksSetsAggregationMetaDataProvider networksSetsAggregationMetaDataProvider) {
    this.networkAggregationMetaDataProvider = networkAggregationMetaDataProvider;
    this.networkTaxonomyMetaDataProvider = networkTaxonomyMetaDataProvider;
    this.networksSetsAggregationMetaDataProvider = networksSetsAggregationMetaDataProvider;
  }

  @Override
  public String getSearchIndex() {
    return Indexer.PUBLISHED_NETWORK_INDEX;
  }

  @Override
  public String getSearchType() {
    return Indexer.NETWORK_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        return publishedNetworkService.getNetworkService().findPublishedIds().stream()
            .filter(s -> subjectAclService.isAccessible("/network", s))
            .collect(Collectors.toList());
      }
    };
  }

  @Override
  protected Taxonomy getTaxonomy() {
    return taxonomiesService.getNetworkTaxonomy();
  }

  @Nullable
  @Override
  protected Properties getAggregationsProperties(List<String> filter) {
    Properties properties = getAggregationsProperties(filter, taxonomiesService.getNetworkTaxonomy());
    if (!properties.containsKey(JOIN_FIELD)) properties.put(JOIN_FIELD, "");
    return properties;
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, Searcher.DocumentResults results, QueryScope scope, CountStatsData counts)
      throws IOException {
    NetworkResultDto.Builder resBuilder = NetworkResultDto.newBuilder();
    NetworkCountStatsBuilder networkCountStatsBuilder = counts == null
        ? null
        : NetworkCountStatsBuilder.newBuilder(counts);

    Consumer<Network> addDto = networkConsumer(scope, resBuilder, networkCountStatsBuilder);
    List<Network> networks = Lists.newArrayList();

    for (Searcher.DocumentResult result : results.getDocuments()) {
      if (result.hasSource())
        networks.add(objectMapper.readValue(result.getSourceInputStream(), Network.class));
    }

    networks.forEach(addDto);
    builder.setExtension(NetworkResultDto.result, resBuilder.build());
  }

  private Consumer<Network> networkConsumer(QueryScope scope, NetworkResultDto.Builder resBuilder,
                                            NetworkCountStatsBuilder networkCountStatsBuilder) {

    return scope == QueryScope.DETAIL ? networkConsumer(resBuilder, networkCountStatsBuilder) : (network) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(network).build());
  }

  private Consumer<Network> networkConsumer(NetworkResultDto.Builder resBuilder, NetworkCountStatsBuilder networkCountStatsBuilder) {
    return (network) -> {
      Mica.NetworkDto.Builder networkBuilder = mode == QueryMode.LIST || mode == QueryMode.SEARCH
        ? dtos.asDtoBuilderForSearchListing(network)
        : dtos.asDtoBuilder(network);

      if (networkCountStatsBuilder != null) {
        networkBuilder.setExtension(MicaSearch.CountStatsDto.networkCountStats, networkCountStatsBuilder.build(network))
            .build();
      }
      resBuilder.addNetworks(networkBuilder.build());
    };
  }

  @Override
  protected List<String> getMandatorySourceFields() {
    return Lists.newArrayList(
      "id",
      "studyIds"
    );
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  @Override
  protected List<AggregationMetaDataProvider> getAggregationMetaDataProviders() {
    return Arrays.asList(networkAggregationMetaDataProvider, networkTaxonomyMetaDataProvider, networksSetsAggregationMetaDataProvider);
  }

  public Map<String, List<String>> getStudyCountsByNetwork() {
    Map<String, List<String>> map = Maps.newHashMap();
    if (!micaConfigService.getConfig().isNetworkEnabled()) return map;

    Properties props = new Properties();
    props.setProperty("id", "");
    Properties subProps = new Properties();
    subProps.setProperty(JOIN_FIELD, "");
    Map<String, Properties> subAggregations = Maps.newHashMap();
    subAggregations.put("id", subProps);


    try {
      Searcher.DocumentResults results = searcher.cover(getSearchIndex(), getSearchType(), getQuery(), props, subAggregations, null);
      results.getAggregations().stream().filter(agg -> AggregationHelper.isTermsAgg(agg.getType()))
          .forEach(
              aggregation -> aggregation.asTerms().getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
                  .forEach(bucket -> map.put(bucket.getKeyAsString(), getStudyCounts(bucket.getAggregations()))));
    } catch (Exception e) {
      log.warn("Study counts by network failed: {}", e.getMessage());
      if (log.isDebugEnabled())
        log.error("Study counts by network failed", e);
    }
    return map;
  }

  private List<String> getStudyCounts(List<Searcher.DocumentAggregation> aggregations) {
    List<String> list = Lists.newArrayList();
    aggregations.stream().filter(agg -> AggregationHelper.isTermsAgg(agg.getType()))
        .forEach(
            aggregation -> aggregation.asTerms().getBuckets().stream().filter(bucket -> bucket.getDocCount() > 0)
                .forEach(bucket -> list.add(bucket.getKeyAsString())));
    return list;
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }
}
