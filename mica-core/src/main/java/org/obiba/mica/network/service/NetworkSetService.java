package org.obiba.mica.network.service;

import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.network.domain.Network;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class NetworkSetService extends DocumentSetService {

  private final PublishedNetworkService publishedNetworkService;

  @Inject
  public NetworkSetService(PublishedNetworkService publishedNetworkService) {
    this.publishedNetworkService = publishedNetworkService;
  }

  @Override
  public String getType() {
    return "Network";
  }

  /**
   * Get a subset of the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @param from
   * @param limit
   * @return
   */
  public List<Network> getPublishedNetworks(DocumentSet documentSet, int from, int limit) {
    ensureType(documentSet);
    if (documentSet.getIdentifiers().isEmpty()) return Lists.newArrayList();
    List<String> ids = Lists.newArrayList(documentSet.getIdentifiers());
    Collections.sort(ids);
    int to = from + limit;
    if (to > ids.size()) to = ids.size();
    return publishedNetworkService.findByIds(ids.subList(from, to));
  }

  /**
   * Get studies from their identifiers.
   *
   * @param identifiers
   * @param useCache
   * @return
   */
  public List<Network> getPublishedNetworks(Set<String> identifiers, boolean useCache) {
    return publishedNetworkService.findByIds(Lists.newArrayList(identifiers), useCache);
  }

  /**
   * Get the variables referred by the {@link DocumentSet}.
   *
   * @param documentSet
   * @return
   */
  public List<Network> getPublishedNetworks(DocumentSet documentSet, boolean useCache) {
    return getPublishedNetworks(documentSet.getIdentifiers(), useCache);
  }
}
