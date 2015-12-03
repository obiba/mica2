/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsPublishedNetworkService extends AbstractPublishedDocumentService<Network>
    implements PublishedNetworkService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private NetworkService networkService;

  @Override
  public NetworkService getNetworkService() {
    return networkService;
  }

  @Override
  protected Network processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, Network.class);
  }

  @Override
  protected String getIndexName() {
    return NetworkIndexer.PUBLISHED_NETWORK_INDEX;
  }

  @Override
  protected String getType() {
    return NetworkIndexer.NETWORK_TYPE;
  }

  @Override
  protected FilterBuilder filterByAccess() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = networkService.findPublishedStates().stream().map(NetworkState::getId)
      .filter(s -> subjectAclService.isAccessible("/network", s))
      .collect(Collectors.toList());
    return ids.isEmpty()
      ? FilterBuilders.notFilter(FilterBuilders.existsFilter("id"))
      : FilterBuilders.idsFilter().ids(ids.toArray(new String[ids.size()]));
  }
}
