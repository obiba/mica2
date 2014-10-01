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

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.search.NetworkIndexer;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.CountStatsData;
import org.obiba.mica.search.CountStatsDtoBuilders;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class NetworkQuery extends AbstractDocumentQuery {

  private static final String NETWORK_FACETS_YML = "network-facets.yml";

  private static final String JOIN_FIELD = "studyIds";

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
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(NETWORK_FACETS_YML);
  }

  @Override
  public void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits, CountStatsData counts) {
    if (counts == null) {
      processHits(builder, hits);
      return;
    }

    MicaSearch.NetworkResultDto.Builder resBuilder = MicaSearch.NetworkResultDto.newBuilder();
    CountStatsDtoBuilders.NetworkCountStatsBuilder networkCountStatsBuilder
        = CountStatsDtoBuilders.NetworkCountStatsBuilder.newBuilder(counts);

    for(SearchHit hit : hits) {
      Network network = publishedNetworkService.findById(hit.getId());
      resBuilder.addNetworks(dtos.asDtoBuilder(network)
          .setExtension(MicaSearch.CountStatsDto.networkCountStats, networkCountStatsBuilder.build(network)).build());
    }

    builder.setExtension(MicaSearch.NetworkResultDto.result, resBuilder.build());
  }

  private void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits) {
    MicaSearch.NetworkResultDto.Builder resBuilder = MicaSearch.NetworkResultDto.newBuilder();
    for(SearchHit hit : hits) {
      resBuilder.addNetworks(dtos.asDto(publishedNetworkService.findById(hit.getId())));
    }
    builder.setExtension(MicaSearch.NetworkResultDto.result, resBuilder.build());
  }


  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  @Override
  public Map<String, Integer> getStudyCounts() {
    return getStudyCounts(JOIN_FIELD);
  }

}
