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

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
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
  private Client client;

  @Inject
  private ObjectMapper objectMapper;

  @Override
  public Networks getNetworks(int from, int limit, @Nullable String sort, @Nullable String order,
      @Nullable String studyId) {

      QueryBuilder query = null;
      if(studyId != null) {
        query = QueryBuilders.termQuery("studyIds", studyId);
      }

      SearchRequestBuilder search = client.prepareSearch() //
          .setIndices(NetworkIndexer.PUBLISHED_NETWORK_INDEX) //
          .setTypes(NetworkIndexer.NETWORK_TYPE) //
          .setQuery(query) //
          .setFrom(from) //
          .setSize(limit);

      if(sort != null) {
        search.addSort(
            SortBuilders.fieldSort(sort).order(order == null ? SortOrder.ASC : SortOrder.valueOf(order.toUpperCase())));
      }

      log.info(search.toString());
      SearchResponse response = search.execute().actionGet();
      log.info(response.toString());

      Networks networks = new Networks(Long.valueOf(response.getHits().getTotalHits()).intValue(), from, limit);

      response.getHits().forEach(hit -> {
        InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
        try {
          networks.add(objectMapper.readValue(inputStream, Network.class));
        } catch(IOException e) {
          log.error("Failed retrieving a network", e);
        }
      });

      return networks;
    }

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
