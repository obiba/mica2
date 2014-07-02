package org.obiba.mica.search;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
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

  public IndexResponse index(String indexName, Persistable<String> persistable) {
    createIndexIfNeeded(indexName);
    return getIndexRequestBuilder(indexName, persistable).setSource(toJson(persistable)).execute().actionGet();
  }

  public BulkResponse indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    createIndexIfNeeded(indexName);
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    persistables.forEach(
        persistable -> bulkRequest.add(getIndexRequestBuilder(indexName, persistable).setSource(toJson(persistable))));
    return bulkRequest.execute().actionGet();
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

  public boolean hasIndex(String indexName) {
    return client.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet().isExists();
  }

  public DeleteIndexResponse dropIndex(String indexName) {
    return client.admin().indices().prepareDelete(indexName).execute().actionGet();
  }

  private IndexRequestBuilder getIndexRequestBuilder(String indexName, Persistable<String> persistable) {
    return client.prepareIndex(indexName, persistable.getClass().getSimpleName(), persistable.getId());
  }

  private void createIndexIfNeeded(String indexName) {
    IndicesAdminClient indicesAdmin = client.admin().indices();
    if(!hasIndex(indexName)) {
      log.info("Creating index {}", indexName);
      Settings settings = ImmutableSettings.settingsBuilder() //
          .put("number_of_shards", elasticSearchConfiguration.getNbShards()) //
          .put("number_of_replicas", elasticSearchConfiguration.getNbReplicas()).build();
      indicesAdmin.prepareCreate(indexName).setSettings(settings).execute().actionGet();
    }
  }

}
