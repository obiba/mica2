package org.obiba.mica.search.aggregations;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.springframework.stereotype.Component;

@Component
public class NetworkAggregationMetaDataProvider implements AggregationMetaDataProvider {

  @Inject
  PublishedNetworkService publishedNetworkService;

  private Map<String, LocalizedString> cacheTitles;

  private Map<String, LocalizedString> cacheDescriptions;

  public void refresh() {
    cacheTitles = publishedNetworkService.findAll().stream()
      .collect(Collectors.toMap(Network::getId, Network::getAcronym));
    cacheDescriptions = publishedNetworkService.findAll().stream()
      .collect(Collectors.toMap(Network::getId, Network::getName));
  }

  public MetaData getTitle(String aggregation, String termKey, String locale) {
    return "networkId".equals(aggregation) && cacheTitles.containsKey(termKey)
      ? MetaData.newBuilder().title(cacheTitles.get(termKey).get(locale))
      .description(cacheDescriptions.get(termKey).get(locale)).build()
      : null;
  }

}
