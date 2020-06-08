package org.obiba.mica.network.rest;

import org.obiba.mica.EntityIndexHealthResource;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.List;


@Component
@Path("/networks/index/health")
@Scope("request")
public class NetworksIndexHealthResource extends EntityIndexHealthResource<Network> {

  final private NetworkService networkService;

  final private PublishedNetworkService publishedNetworkService;

  @Inject
  public NetworksIndexHealthResource(NetworkService networkService, PublishedNetworkService publishedNetworkService) {
    this.networkService = networkService;
    this.publishedNetworkService = publishedNetworkService;
  }

  @Override
  protected List<Network> findAllPublished() {
    return networkService.findAllPublishedNetworks();
  }

  @Override
  protected List<String> findAllIndexedIds() {
    return publishedNetworkService.suggest(MAX_VALUE, "en", createEsQuery(Network.class), ES_QUERY_FIELDS);
  }

  @Override
  protected String getEntityTitle(Network entity, String locale) {
    return entity.getAcronym().get(locale);
  }

  @Override
  protected String createEsQuery(Class clazz) {
    return String.format("*");
  }
}
