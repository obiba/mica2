package org.obiba.mica.search.mapping;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.obiba.mica.dataset.event.HarmonizationDatasetIndexedEvent;
import org.obiba.mica.dataset.event.StudyDatasetIndexedEvent;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.dataset.search.VariableIndexer;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.network.search.NetworkIndexer;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.search.StudyIndexer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

/**
 * Service providing Elasticsearch Index Mapping information
 */
@Component
public class IndexFieldMappingService {
  @Inject
  private Client client;

  private Map<String, IndexFieldMapping> mappings;

  @PostConstruct
  public void init() {
    mappings = Maps.newHashMap();
  }

  public IndexFieldMapping getVariableIndexMapping() {
    return getMapping(VariableIndexer.DRAFT_VARIABLE_INDEX, VariableIndexer.VARIABLE_TYPE);
  }

  public IndexFieldMapping getDatasetIndexMapping() {
    return getMapping(DatasetIndexer.DRAFT_DATASET_INDEX, DatasetIndexer.DATASET_TYPE);
  }

  public IndexFieldMapping getStudyIndexMapping() {
    return getMapping(StudyIndexer.DRAFT_STUDY_INDEX, StudyIndexer.STUDY_TYPE);
  }

  public IndexFieldMapping getNetworkIndexMapping() {
    return getMapping(NetworkIndexer.DRAFT_NETWORK_INDEX, NetworkIndexer.NETWORK_TYPE);
  }

  @Async
  @Subscribe
  public void clearNetworkIndexMapping(IndexNetworksEvent event) {
    mappings.remove(NetworkIndexer.DRAFT_NETWORK_INDEX);
  }

  @Async
  @Subscribe
  public void clearStudyIndexMapping(IndexStudiesEvent event) {
    mappings.remove(StudyIndexer.DRAFT_STUDY_INDEX);
  }

  @Async
  @Subscribe
  public void clearStudyDatasetIndexMapping(StudyDatasetIndexedEvent event) {
    clearDatasetIndexMapping();
  }

  @Async
  @Subscribe
  public void clearHarmonizationDatasetIndexMapping(HarmonizationDatasetIndexedEvent event) {
    clearDatasetIndexMapping();
  }

  private void clearDatasetIndexMapping() {
    mappings.remove(DatasetIndexer.DRAFT_DATASET_INDEX);
    mappings.remove(VariableIndexer.DRAFT_VARIABLE_INDEX);
  }

  private IndexFieldMapping getMapping(String name, String type) {
    IndexFieldMapping mapping = mappings.get(name);

    if (mapping == null) {
      mapping = new IndexFieldMappingImpl(getContext(name, type));
      mappings.put(name, mapping);
    }

    return mapping;

  }

  private ReadContext getContext(String indexName, String indexType) {
    GetMappingsResponse result = client.admin().indices().prepareGetMappings(indexName).execute().actionGet();
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = result.getMappings();
    MappingMetaData metaData = mappings.get(indexName).get(indexType);
    Object jsonContent = Configuration.defaultConfiguration().jsonProvider().parse(metaData.source().toString());
    return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST)).parse(jsonContent);
  }

  private static class IndexFieldMappingImpl implements IndexFieldMapping {

    private final ReadContext context;

    IndexFieldMappingImpl(ReadContext ctx) {
      context = ctx;
    }

    @Override
    public boolean isAnalyzed(String fieldName) {
      List<Object> result = context.read(String.format("$..%s..analyzed", fieldName.replaceAll("\\.", "..")));
      return result.size() > 0;
    }

  }

}
