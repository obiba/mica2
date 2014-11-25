/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.search.NetworkIndexer;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static org.obiba.mica.search.CountStatsDtoBuilders.NetworkCountStatsBuilder;
import static org.obiba.mica.web.model.MicaSearch.NetworkResultDto;
import static org.obiba.mica.web.model.MicaSearch.QueryResultDto;

@Component
@Scope("request")
public class NetworkQuery extends AbstractDocumentQuery {

  private static final String NETWORK_FACETS_YML = "network-facets.yml";

  public static final String JOIN_FIELD = "studyIds";

  @Inject
  Dtos dtos;

  @Inject
  PublishedNetworkService publishedNetworkService;

  @Override
  public String getSearchIndex() {
    return NetworkIndexer.PUBLISHED_NETWORK_INDEX;
  }

  @Override
  public String getSearchType() {
    return NetworkIndexer.NETWORK_TYPE;
  }

  @Override
  public Stream<String> getLocalizedQueryStringFields() {
    return Stream.of(NetworkIndexer.LOCALIZED_ANALYZED_FIELDS);
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(NETWORK_FACETS_YML);
  }

  @Override
  public void processHits(QueryResultDto.Builder builder, SearchHits hits, Scope scope, CountStatsData counts) {
    NetworkResultDto.Builder resBuilder = NetworkResultDto.newBuilder();
    NetworkCountStatsBuilder networkCountStatsBuilder = counts == null
        ? null
        : NetworkCountStatsBuilder.newBuilder(counts);

    Consumer<Network> addDto = getNetworkConsumer(scope, resBuilder, networkCountStatsBuilder);

    for(SearchHit hit : hits) {
      addDto.accept(publishedNetworkService.findById(hit.getId()));
    }

    builder.setExtension(NetworkResultDto.result, resBuilder.build());
  }

  private Consumer<Network> getNetworkConsumer(Scope scope, NetworkResultDto.Builder resBuilder,
      NetworkCountStatsBuilder networkCountStatsBuilder) {

    return scope == Scope.DETAIL
      ? (network) -> {
        Mica.NetworkDto.Builder networkBuilder = dtos.asDtoBuilder(network);
        if(networkCountStatsBuilder != null) {
          networkBuilder
              .setExtension(MicaSearch.CountStatsDto.networkCountStats, networkCountStatsBuilder.build(network))
              .build();
        }
        resBuilder.addNetworks(networkBuilder.build());
      }
      : (network) -> resBuilder.addDigests(dtos.asDigestDtoBuilder(network).build());
  }


  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getDocumentCounts(JOIN_FIELD);
  }

}
