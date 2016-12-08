/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.core.domain.Indexable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ElasticSearchIndexer {

  private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexer.class);

  private static final int MAX_SIZE = 10000;

  @Inject
  private ElasticSearchConfiguration elasticSearchConfiguration;

  @Inject
  private Client client;

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private Set<IndexConfigurationListener> indexConfigurationListeners;

  public IndexResponse index(String indexName, Persistable<String> persistable) {
    return index(indexName, persistable, null);
  }

  public IndexResponse index(String indexName, Persistable<String> persistable, Persistable<String> parent) {
    createIndexIfNeeded(indexName);
    String parentId = parent == null ? null : parent.getId();
    return getIndexRequestBuilder(indexName, persistable).setSource(toJson(persistable)).setParent(parentId).execute()
      .actionGet();
  }

  public IndexResponse index(String indexName, Indexable indexable) {
    return index(indexName, indexable, null);
  }

  public IndexResponse index(String indexName, Indexable indexable, Indexable parent) {
    createIndexIfNeeded(indexName);
    String parentId = parent == null ? null : parent.getId();
    return getIndexRequestBuilder(indexName, indexable).setSource(toJson(indexable)).setParent(parentId).execute()
      .actionGet();
  }

  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    indexAll(indexName, persistables, null);
  }

  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables,
    Persistable<String> parent) {
    createIndexIfNeeded(indexName);
    String parentId = parent == null ? null : parent.getId();
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    persistables.forEach(persistable -> bulkRequest
      .add(getIndexRequestBuilder(indexName, persistable).setSource(toJson(persistable)).setParent(parentId)));

    if (bulkRequest.numberOfActions() > 0) bulkRequest.execute().actionGet();
  }

  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    indexAllIndexables(indexName, indexables, (String) null);
  }

  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables,
    @Nullable Indexable parent) {
    String parentId = parent == null ? null : parent.getId();
    indexAllIndexables(indexName, indexables, parentId);
  }

  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId) {
    createIndexIfNeeded(indexName);
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    indexables.forEach(indexable -> bulkRequest
      .add(getIndexRequestBuilder(indexName, indexable).setSource(toJson(indexable)).setParent(parentId)));

    if(bulkRequest.numberOfActions() > 0) bulkRequest.execute().actionGet();
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch(JsonProcessingException e) {
      throw new RuntimeException("Cannot serialize " + obj + " to ElasticSearch", e);
    }
  }

  public DeleteResponse delete(String indexName, Persistable<String> persistable) {
    createIndexIfNeeded(indexName);
    return client.prepareDelete(indexName, persistable.getClass().getSimpleName(), persistable.getId()).execute()
      .actionGet();
  }

  public DeleteResponse delete(String indexName, Indexable indexable) {
    createIndexIfNeeded(indexName);
    return client.prepareDelete(indexName, getType(indexable), indexable.getId()).execute().actionGet();
  }

  public ActionResponse delete(String indexName, String[] types, QueryBuilder query) {
    ActionResponse lastResponse = null;

    if (types != null) {
      createIndexIfNeeded(indexName);

      BulkRequestBuilder bulkRequest = client.prepareBulk();

      SearchRequestBuilder search = client.prepareSearch() //
        .setIndices(indexName) //
        .setTypes(types) //
        .setQuery(query) //
        .setSize(MAX_SIZE) //
        .setNoFields();

      SearchResponse response = search.execute().actionGet();

      for(SearchHit hit : response.getHits()) {
        for(String type: types) {
          DeleteRequestBuilder request = client.prepareDelete(indexName, type, hit.getId());
          if (hit.getFields() != null && hit.getFields().containsKey("_parent")) {
            String parent = hit.field("_parent").value();
            request.setParent(parent);
          }

          bulkRequest.add(request);
        }
      }

      try {
        lastResponse = bulkRequest.execute().get();
      } catch (InterruptedException | ExecutionException e) {
        //
      }
    }

    return lastResponse;
  }

  public ActionResponse delete(String indexName, String type, QueryBuilder query) {
    return delete(indexName, type != null ? new String[] {type} : null, query);
  }

  public boolean hasIndex(String indexName) {
    return client.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet().isExists();
  }

  public DeleteIndexResponse dropIndex(String indexName) {
    return client.admin().indices().prepareDelete(indexName).execute().actionGet();
  }

  public Client getClient() {
    return client;
  }

  //
  // Private methods
  //

  private IndexRequestBuilder getIndexRequestBuilder(String indexName, Persistable<String> persistable) {
    return client.prepareIndex(indexName, persistable.getClass().getSimpleName(), persistable.getId());
  }

  private IndexRequestBuilder getIndexRequestBuilder(String indexName, Indexable indexable) {
    return client.prepareIndex(indexName, getType(indexable), indexable.getId());
  }

  private String getType(Indexable indexable) {
    return indexable.getMappingName() == null ? indexable.getClassName() : indexable.getMappingName();
  }

  private synchronized void createIndexIfNeeded(String indexName) {
    log.trace("Ensuring index existence for index {}", indexName);
    IndicesAdminClient indicesAdmin = client.admin().indices();
    if(!hasIndex(indexName)) {
      log.info("Creating index {}", indexName);

      Settings settings = Settings.builder() //
        .put(elasticSearchConfiguration.getIndexSettings())
        .put("number_of_shards", elasticSearchConfiguration.getNbShards()) //
        .put("number_of_replicas", elasticSearchConfiguration.getNbReplicas()).build();

      indicesAdmin.prepareCreate(indexName).setSettings(settings).execute().actionGet();

      if(indexConfigurationListeners != null) {
        indexConfigurationListeners.forEach(listener -> listener.onIndexCreated(client, indexName));
      }
    }
  }

  public interface IndexConfigurationListener {
    void onIndexCreated(Client client, String indexName);
  }
}
