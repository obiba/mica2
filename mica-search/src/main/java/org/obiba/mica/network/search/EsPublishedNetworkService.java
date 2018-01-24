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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class EsPublishedNetworkService extends AbstractDocumentService<Network>
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
  protected Network processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), Network.class);
  }

  @Override
  protected String getIndexName() {
    return Indexer.PUBLISHED_NETWORK_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.NETWORK_TYPE;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        return networkService.findPublishedIds().stream()
            .filter(s -> subjectAclService.isAccessible("/network", s))
            .collect(Collectors.toList());
      }
    };
  }
}
