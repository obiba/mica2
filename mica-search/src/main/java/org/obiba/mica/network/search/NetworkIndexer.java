  /*
   * Copyright (c) 2018 OBiBa. All rights reserved.
   *
   * This program and the accompanying materials
   * are made available under the terms of the GNU Public License v3.0.
   *
   * You should have received a copy of the GNU General Public License
   * along with this program.  If not, see <http://www.gnu.org/licenses/>.
   */

  package org.obiba.mica.network.search;

  import com.google.common.collect.Lists;
  import com.google.common.eventbus.Subscribe;
  import org.obiba.mica.core.domain.DocumentSet;
  import org.obiba.mica.core.domain.Membership;
  import org.obiba.mica.core.event.DocumentSetDeletedEvent;
  import org.obiba.mica.core.event.DocumentSetUpdatedEvent;
  import org.obiba.mica.core.service.PersonService;
  import org.obiba.mica.network.domain.Network;
  import org.obiba.mica.network.event.*;
  import org.obiba.mica.network.service.NetworkService;
  import org.obiba.mica.network.service.NetworkSetService;
  import org.obiba.mica.spi.search.Indexer;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.scheduling.annotation.Async;
  import org.springframework.stereotype.Component;

  import javax.inject.Inject;
  import java.util.List;
  import java.util.Map;
  import java.util.stream.Collectors;

  @Component
  public class NetworkIndexer {

    private static final Logger log = LoggerFactory.getLogger(NetworkIndexer.class);

    private final NetworkService networkService;

    private final PersonService personService;

    private final Indexer indexer;

    private final NetworkSetService networkSetService;

    @Inject
    public NetworkIndexer(NetworkService networkService, PersonService personService, Indexer indexer, NetworkSetService networkSetService) {
      this.networkService = networkService;
      this.personService = personService;
      this.indexer = indexer;
      this.networkSetService = networkSetService;
    }

    @Async
    @Subscribe
    public void networkUpdated(NetworkUpdatedEvent event) {
      log.info("Network {} was updated", event.getPersistable());
      indexer.index(Indexer.DRAFT_NETWORK_INDEX, decorate(event.getPersistable()));
    }

    @Async
    @Subscribe
    public void networkPublished(NetworkPublishedEvent event) {
      log.info("Network {} was published", event.getPersistable());
      indexer.index(Indexer.PUBLISHED_NETWORK_INDEX, decorate(event.getPersistable()));
    }

    @Async
    @Subscribe
    public void networkPublished(NetworkUnpublishedEvent event) {
      log.info("Network {} was unpublished", event.getPersistable());
      indexer.delete(Indexer.PUBLISHED_NETWORK_INDEX, event.getPersistable());
    }

    @Async
    @Subscribe
    public void networkDeleted(NetworkDeletedEvent event) {
      log.info("Network {} was deleted", event.getPersistable());
      indexer.delete(Indexer.DRAFT_NETWORK_INDEX, event.getPersistable());
      indexer.delete(Indexer.PUBLISHED_NETWORK_INDEX, event.getPersistable());
    }

    @Async
    @Subscribe
    public void reIndexNetworks(IndexNetworksEvent event) {
      log.info("Reindexing all networks");
      List<String> networkIds = event.getIds();

      if (networkIds.isEmpty()) {
        reIndexAll(Indexer.PUBLISHED_NETWORK_INDEX, decorate(networkService.findAllPublishedNetworks()));
        reIndexAll(Indexer.DRAFT_NETWORK_INDEX, decorate(networkService.findAllNetworks()));
      } else {
        // indexAll does not deletes the index before
        indexer.indexAll(Indexer.PUBLISHED_NETWORK_INDEX, decorate(networkService.findAllPublishedNetworks(networkIds)));
        indexer.indexAll(Indexer.DRAFT_NETWORK_INDEX, decorate(networkService.findAllNetworks(networkIds)));
      }
    }

    @Async
    @Subscribe
    public synchronized void documentSetUpdated(DocumentSetUpdatedEvent event) {
      if (!networkSetService.isForType(event.getPersistable())) return;
      List<Network> toIndex = Lists.newArrayList();
      String id = event.getPersistable().getId();
      if (event.hasRemovedIdentifiers()) {
        List<Network> toRemove = networkSetService.getPublishedNetworks(event.getRemovedIdentifiers(), false);
        toRemove.forEach(ntw -> ntw.removeSet(id));
        toIndex.addAll(toRemove);
      }
      List<Network> networks = networkSetService.getPublishedNetworks(event.getPersistable(), false);
      networks.stream()
        .filter(ntw -> !ntw.containsSet(id))
        .forEach(ntw -> {
          ntw.addSet(id);
          toIndex.add(ntw);
        });
      indexer.indexAll(Indexer.PUBLISHED_NETWORK_INDEX, toIndex);
    }

    @Async
    @Subscribe
    public synchronized void documentSetDeleted(DocumentSetDeletedEvent event) {
      if (!networkSetService.isForType(event.getPersistable())) return;
      DocumentSet documentSet = event.getPersistable();
      List<Network> toIndex = Lists.newArrayList();
      {
        List<Network> toRemove = networkSetService.getPublishedNetworks(event.getPersistable(), false);
        toRemove.forEach(ntw -> ntw.removeSet(documentSet.getId()));
        toIndex.addAll(toRemove);
      }
      indexer.indexAll(Indexer.PUBLISHED_NETWORK_INDEX, toIndex);
    }

    //
    // Private methods
    //

    private void reIndexAll(String indexName, Iterable<Network> networks) {
      indexer.reindexAll(indexName, networks);
    }

    private List<Network> decorate(List<Network> networks) {
      return addMemberships(addSets(networks));
    }

    private Network decorate(Network network) {
      return addMemberships(addSets(network));
    }

    private List<Network> addMemberships(List<Network> networks) {
      return networks.stream()
        .map(this::addMemberships)
        .collect(Collectors.toList());
    }

    private Network addMemberships(Network network) {
      Map<String, List<Membership>> membershipMap = personService.getNetworkMembershipMap(network.getId());
      personService.setMembershipOrder(network.getMembershipSortOrder(), membershipMap);

      network.setMemberships(membershipMap);

      return network;
    }

    private Network addSets(Network network) {
      networkSetService.getAll().forEach(ds -> {
        if (ds.getIdentifiers().contains(network.getId())) network.addSet(ds.getId());
      });
      return network;
    }

    private List<Network> addSets(List<Network> networks) {
      List<DocumentSet> documentSets = networkSetService.getAll();
      networks.forEach(ntw ->
        documentSets.forEach(ds -> {
          if (ds.getIdentifiers().contains(ntw.getId())) ntw.addSet(ds.getId());
        }));
      return networks;
    }
  }
