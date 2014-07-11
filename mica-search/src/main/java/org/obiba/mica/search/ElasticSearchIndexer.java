package org.obiba.mica.search;

import java.util.Set;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilder;
import org.obiba.mica.domain.Indexable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ElasticSearchIndexer {

  private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexer.class);

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

  public BulkResponse indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    return indexAll(indexName, persistables, null);
  }

  public BulkResponse indexAll(String indexName, Iterable<? extends Persistable<String>> persistables,
      Persistable<String> parent) {
    createIndexIfNeeded(indexName);
    String parentId = parent == null ? null : parent.getId();
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    persistables.forEach(persistable -> bulkRequest
        .add(getIndexRequestBuilder(indexName, persistable).setSource(toJson(persistable)).setParent(parentId)));
    return bulkRequest.execute().actionGet();
  }

  public BulkResponse indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    return indexAllIndexables(indexName, indexables, null);
  }

  public BulkResponse indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, Indexable parent) {
    createIndexIfNeeded(indexName);
    String parentId = parent == null ? null : parent.getId();
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    indexables.forEach(indexable -> bulkRequest
        .add(getIndexRequestBuilder(indexName, indexable).setSource(toJson(indexable)).setParent(parentId)));
    return bulkRequest.execute().actionGet();
  }

  public static Indexable asIndexable(final Persistable<String> persistable) {
    return new Indexable() {
      @Override
      public String getId() {
        return persistable.getId();
      }

      @Override
      public String getClassName() {
        return getClass().getSimpleName();
      }

      @Override
      public String getMappingName() {
        return getClassName();
      }
    };
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

  public DeleteByQueryResponse delete(String indexName, String type, QueryBuilder query) {
    createIndexIfNeeded(indexName);
    return client.prepareDeleteByQuery(indexName).setTypes(type).setQuery(query).execute().actionGet();
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
    IndicesAdminClient indicesAdmin = client.admin().indices();
    if(!hasIndex(indexName)) {
      log.info("Creating index {}", indexName);
      Settings settings = ImmutableSettings.settingsBuilder() //
          .put("number_of_shards", elasticSearchConfiguration.getNbShards()) //
          .put("number_of_replicas", elasticSearchConfiguration.getNbReplicas()).build();

      indicesAdmin.prepareCreate(indexName).setSettings(settings).execute().actionGet();
      if (indexConfigurationListeners != null) {
        indexConfigurationListeners.forEach(listener -> listener.onIndexCreated(client, indexName));
      }
    }
  }

  public static interface IndexConfigurationListener {
    void onIndexCreated(Client client, String indexName);
  }

}
