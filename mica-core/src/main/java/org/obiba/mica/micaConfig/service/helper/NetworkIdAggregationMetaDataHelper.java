/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import com.google.common.collect.Maps;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.obiba.mica.security.SubjectUtils.sudo;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class NetworkIdAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  private static final Logger log = getLogger(NetworkIdAggregationMetaDataHelper.class);

  @Inject
  PublishedNetworkService publishedNetworkService;

  @Cacheable(value = "aggregations-metadata", key = "'network'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getNetworks() {
    try {
      List<Network> networks = sudo(() -> publishedNetworkService.findAll());
      return networks.stream()
          .collect(Collectors
              .toMap(Network::getId, d -> new AggregationMetaDataProvider.LocalizedMetaData(d.getAcronym(), d.getName(), d.getClass().getSimpleName())));
    } catch (Exception e) {
      log.debug("Could not build Network aggregation metadata {}", e);
      return Maps.newHashMap();
    }
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getNetworks();
  }
}
