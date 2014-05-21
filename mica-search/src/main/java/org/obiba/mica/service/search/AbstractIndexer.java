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

import com.google.gson.Gson;

public abstract class AbstractIndexer {

  @SuppressWarnings("NonConstantLogger")
  protected final Logger log = LoggerFactory.getLogger(AbstractIndexer.class);

  @Inject
  protected ElasticSearchService elasticSearchService;

  @Inject
  private Gson gson;

  protected abstract String getIndexName();

  protected IndexResponse index(Object obj) {
    getOrCreateIndex();
    return elasticSearchService.getClient().prepareIndex(getIndexName(), obj.getClass().getSimpleName())
        .setSource(gson.toJson(obj)).execute().actionGet();
  }

  private IndexMetaData getOrCreateIndex() {
    IndicesAdminClient indicesAdmin = elasticSearchService.getClient().admin().indices();
    if(!indicesAdmin.exists(new IndicesExistsRequest(getIndexName())).actionGet().isExists()) {
      log.info("Creating index {}", getIndexName());
      indicesAdmin.prepareCreate(getIndexName()).setSettings(getIndexSettings()).execute().actionGet();
    }
    return elasticSearchService.getClient().admin().cluster().prepareState().setIndices(getIndexName()).execute()
        .actionGet().getState().getMetaData().index(getIndexName());
  }

  private Settings getIndexSettings() {
    return ImmutableSettings.settingsBuilder() //
        .put("number_of_shards", elasticSearchService.getNbShards()) //
        .put("number_of_replicas", elasticSearchService.getNbReplicas()).build();
  }
}
