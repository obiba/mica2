package org.obiba.mica.network.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.network.NetworkStateRepository;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Path("/networks/index/health")
@Scope("request")
public class NetworksIndexHealthResource extends EntityIndexHealthResource<Network> {

  final private NetworkService networkService;

  final private NetworkStateRepository networkStateRepository;

  final private PublishedNetworkService publishedNetworkService;

  @Inject
  public NetworksIndexHealthResource(NetworkService networkService,
                                     NetworkStateRepository networkStateRepository,
                                     PublishedNetworkService publishedNetworkService) {
    this.networkService = networkService;
    this.networkStateRepository = networkStateRepository;
    this.publishedNetworkService = publishedNetworkService;
  }

  @Override
  protected List<Network> findAllPublished() {
    List<String> ids = networkStateRepository.findByPublishedTagNotNull()
      .stream()
      .map(NetworkState::getId)
      .collect(Collectors.toList());
    return networkService.findAllPublishedNetworks(ids);
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedNetworkService.suggest(MAX_VALUE, "en", createEsQuery(Network.class), ES_QUERY_FIELDS, null);
  }

  @Override
  protected LocalizedString getEntityTitle(Network entity) {
    return entity.getAcronym();
  }

  @Override
  protected String createEsQuery(Class clazz) {
    return String.format("*");
  }
}
