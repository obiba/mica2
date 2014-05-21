package org.obiba.mica.service.search;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class ElasticSearchIndexer {

  private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexer.class);

  @Inject
  private ElasticSearchService elasticSearchService;

  @Inject
  private Gson gson;

  public IndexResponse index(String indexName, Persistable<String> persistable) {
    getOrCreateIndex(indexName);
    return elasticSearchService.getClient().prepareIndex(indexName, persistable.getClass().getSimpleName())
        .setSource(gson.toJson(persistable)).setId(persistable.getId()).execute().actionGet();
  }

  public void indexAll(String indexName, Iterable<Persistable<String>> iterable) {
  }

  private IndexMetaData getOrCreateIndex(String indexName) {
    IndicesAdminClient indicesAdmin = elasticSearchService.getClient().admin().indices();
    if(!indicesAdmin.exists(new IndicesExistsRequest(indexName)).actionGet().isExists()) {
      log.info("Creating index {}", indexName);
      indicesAdmin.prepareCreate(indexName).setSettings(getIndexSettings()).execute().actionGet();
    }
    return elasticSearchService.getClient().admin().cluster().prepareState().setIndices(indexName).execute().actionGet()
        .getState().getMetaData().index(indexName);
  }

  private Settings getIndexSettings() {
    return ImmutableSettings.settingsBuilder() //
        .put("number_of_shards", elasticSearchService.getNbShards()) //
        .put("number_of_replicas", elasticSearchService.getNbReplicas()).build();
  }
}
