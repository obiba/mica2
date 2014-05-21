package org.obiba.mica.service.search;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIndexer {

  @SuppressWarnings("NonConstantLogger")
  protected final Logger log = LoggerFactory.getLogger(AbstractIndexer.class);

  @Inject
  protected ElasticSearchService elasticSearchService;

  protected abstract String getIndexName();

  protected IndexMetaData createIndex() {
    IndicesAdminClient idxAdmin = elasticSearchService.getClient().admin().indices();
    if(!idxAdmin.exists(new IndicesExistsRequest(getIndexName())).actionGet().isExists()) {
      log.info("Creating index {}", getIndexName());
      idxAdmin.prepareCreate(getIndexName()).setSettings(getIndexSettings()).execute().actionGet();
    }
    return elasticSearchService.getClient().admin().cluster().prepareState().setIndices(getIndexName()).execute()
        .actionGet().getState().getMetaData().index(getIndexName());
  }

  protected Settings getIndexSettings() {
    return ImmutableSettings.settingsBuilder() //
        .put("number_of_shards", elasticSearchService.getNbShards()) //
        .put("number_of_replicas", elasticSearchService.getNbReplicas()).build();
  }
}
