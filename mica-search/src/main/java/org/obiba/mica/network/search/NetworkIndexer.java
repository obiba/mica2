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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.service.PersonService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.network.event.NetworkUpdatedEvent;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class NetworkIndexer {

  private static final Logger log = LoggerFactory.getLogger(NetworkIndexer.class);

  @Inject
  private NetworkService networkService;

  @Inject
  private PersonService personService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Indexer indexer;

  @Async
  @Subscribe
  public void networkUpdated(NetworkUpdatedEvent event) {
    log.info("Network {} was updated", event.getPersistable());
    indexer.index(Indexer.DRAFT_NETWORK_INDEX, addMemberships(event.getPersistable()));
  }

  @Async
  @Subscribe
  public void networkPublished(NetworkPublishedEvent event) {
    log.info("Network {} was published", event.getPersistable());
    indexer.index(Indexer.PUBLISHED_NETWORK_INDEX, addMemberships(event.getPersistable()));
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
    reIndexAll(Indexer.PUBLISHED_NETWORK_INDEX, networkService.findAllPublishedNetworks().stream().map(this::addMemberships).collect(Collectors.toList()));
    reIndexAll(Indexer.DRAFT_NETWORK_INDEX, networkService.findAllNetworks().stream().map(this::addMemberships).collect(Collectors.toList()));
  }

  private void reIndexAll(String indexName, Iterable<Network> networks) {
    indexer.reindexAll(indexName, networks);
  }

  private Network addMemberships(Network network) {
    HashMap<String, List<Membership>> membershipMap = new HashMap<String, List<Membership>>();
    micaConfigService.getConfig().getRoles().forEach(role -> membershipMap.put(role, new ArrayList<>()));

    List<Person> networkMemberships = personService.getNetworkMemberships(network.getId());
    networkMemberships.forEach(person -> {
      person.getNetworkMemberships().stream()
        .filter(networkMembership -> networkMembership.getParentId().equals(network.getId()))
        .forEach(networkMembership -> {
          Membership membership = new Membership(person, networkMembership.getRole());
          if (!membershipMap.containsKey(networkMembership.getRole())) {
            membershipMap.put(networkMembership.getRole(), Lists.newArrayList(membership));
          } else {
            membershipMap.get(networkMembership.getRole()).add(membership);
          }
        });
    });

    membershipMap.forEach((role, people) -> {
      people.sort(new Comparator<Membership>() {
        @Override
        public int compare(Membership membership1, Membership membership2) {
          List<String> list = network.getMembershipSortOrder().get(role);
          return list.indexOf(membership1) - list.indexOf(membership2);
        }
      });
    });

    network.setMemberships(membershipMap);

    return network;
  }
}
