package org.obiba.mica.search.aggregations;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class NetworkAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "networkId";

  @Inject
  AggregationMetaDataHelper helper;

  @Override
  public void refresh() {
  }

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> networks = helper.getNetworks();
    return AGGREGATION_NAME.equals(aggregation) && networks.containsKey(termKey)
      ? MetaData.newBuilder().title(networks.get(termKey).getTitle().get(locale))
      .description(networks.get(termKey).getDescription().get(locale)).build()
      : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

  @Component
  public static class AggregationMetaDataHelper {

    @Inject
    PublishedNetworkService publishedNetworkService;

    @Cacheable(value="aggregations-metadata", key = "'network'")
    public Map<String, LocalizedMetaData> getNetworks() {
      List<Network> networks = publishedNetworkService.findAll();
      return networks.stream()
        .collect(Collectors
          .toMap(Network::getId, d -> new LocalizedMetaData(d.getAcronym(), d.getName())));
    }
  }
}
