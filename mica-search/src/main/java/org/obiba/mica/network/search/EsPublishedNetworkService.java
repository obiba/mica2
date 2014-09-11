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

import javax.inject.Inject;

import org.elasticsearch.search.SearchHits;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Service
public class EsPublishedNetworkService  extends AbstractPublishedDocumentService<Network> implements
    PublishedNetworkService {

  private static final Logger log = LoggerFactory.getLogger(EsPublishedNetworkService.class);

  @Inject
  private ObjectMapper objectMapper;

  @Override
  protected List<Network> processHits(SearchHits hits) {
    List<Network> networks = Lists.newArrayList();
    hits.forEach(hit -> {
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      try {
        networks.add(objectMapper.readValue(inputStream, Network.class));
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    });

    return networks;
  }

  @Override
  protected String getIndexName() {
    return NetworkIndexer.PUBLISHED_NETWORK_INDEX;
  }

  @Override
  protected String getType() {
    return NetworkIndexer.NETWORK_TYPE;
  }
}
