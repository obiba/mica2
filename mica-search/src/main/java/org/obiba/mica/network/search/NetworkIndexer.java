/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.search;

import javax.inject.Inject;

import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.network.event.NetworkUpdatedEvent;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class NetworkIndexer {

  private static final Logger log = LoggerFactory.getLogger(NetworkIndexer.class);

  public static final String DRAFT_NETWORK_INDEX = "network-draft";

  public static final String PUBLISHED_NETWORK_INDEX = "network-published";

  public static final String NETWORK_TYPE = "Network";

  public static final String[] LOCALIZED_ANALYZED_FIELDS = {"acronym", "name", "description"};

  @Inject
  private NetworkService networkService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void networkUpdated(NetworkUpdatedEvent event) {
    log.info("Network {} was updated", event.getPersistable());
    elasticSearchIndexer.index(DRAFT_NETWORK_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void networkPublished(NetworkPublishedEvent event) {
    log.info("Network {} was published", event.getPersistable());
    elasticSearchIndexer.index(PUBLISHED_NETWORK_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void networkPublished(NetworkUnpublishedEvent event) {
    log.info("Network {} was unpublished", event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_NETWORK_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void networkDeleted(NetworkDeletedEvent event) {
    log.info("Network {} was deleted", event.getPersistable());
    elasticSearchIndexer.delete(DRAFT_NETWORK_INDEX, event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_NETWORK_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexNetworks(IndexNetworksEvent event) {
    log.info("Reindexing all networks");
    reIndexAll(PUBLISHED_NETWORK_INDEX, networkService.findAllPublishedNetworks());
    reIndexAll(DRAFT_NETWORK_INDEX, networkService.findAllNetworks());
  }

  private void reIndexAll(String indexName, Iterable<Network> networks) {
    elasticSearchIndexer.reindexAll(indexName, networks);
  }
}
