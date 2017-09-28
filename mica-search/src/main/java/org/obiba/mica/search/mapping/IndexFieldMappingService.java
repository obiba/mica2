/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.mapping;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.obiba.mica.dataset.event.HarmonizationDatasetIndexedEvent;
import org.obiba.mica.dataset.event.StudyDatasetIndexedEvent;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Service providing Elasticsearch Index Mapping information
 */
@Component
public class IndexFieldMappingService {

  @Inject
  private Searcher searcher;

  private Map<String, IndexFieldMapping> mappings;

  @PostConstruct
  public void init() {
    mappings = Maps.newHashMap();
  }

  public IndexFieldMapping getVariableIndexMapping() {
    return getMapping(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE);
  }

  public IndexFieldMapping getDatasetIndexMapping() {
    return getMapping(Indexer.DRAFT_DATASET_INDEX, Indexer.DATASET_TYPE);
  }

  public IndexFieldMapping getStudyIndexMapping() {
    return getMapping(Indexer.DRAFT_STUDY_INDEX, Indexer.STUDY_TYPE);
  }

  public IndexFieldMapping getNetworkIndexMapping() {
    return getMapping(Indexer.DRAFT_NETWORK_INDEX, Indexer.NETWORK_TYPE);
  }

  @Async
  @Subscribe
  public void clearNetworkIndexMapping(IndexNetworksEvent event) {
    mappings.remove(Indexer.DRAFT_NETWORK_INDEX);
  }

  @Async
  @Subscribe
  public void clearStudyIndexMapping(IndexStudiesEvent event) {
    mappings.remove(Indexer.DRAFT_STUDY_INDEX);
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
    mappings.remove(Indexer.DRAFT_DATASET_INDEX);
    mappings.remove(Indexer.PUBLISHED_VARIABLE_INDEX);
  }

  private IndexFieldMapping getMapping(String name, String type) {
    IndexFieldMapping mapping = mappings.get(name);
    Function<String, Boolean> indexExists = (n) -> searcher.admin().indices().prepareExists(n).get().isExists();

    if (mapping == null) {
      mapping = new IndexFieldMappingImpl(indexExists.apply(name) ? getContext(name, type) : null);
      mappings.put(name, mapping);
    }

    return mapping;

  }

  private ReadContext getContext(String indexName, String indexType) {
    GetMappingsResponse result = searcher.admin().indices().prepareGetMappings(indexName).execute().actionGet();
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = result.getMappings();
    MappingMetaData metaData = mappings.get(indexName).get(indexType);
    Object jsonContent = Configuration.defaultConfiguration().jsonProvider().parse(metaData.source().toString());
    return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST)).parse(jsonContent);
  }

  private static class IndexFieldMappingImpl implements IndexFieldMapping {

    private Optional<ReadContext> context;

    IndexFieldMappingImpl(ReadContext ctx) {
      context = Optional.ofNullable(ctx);
    }

    @Override
    public boolean isAnalyzed(String fieldName) {
      boolean analyzed = false;
      if (context.isPresent()) {
        List<Object> result = context.get().read(String.format("$..%s..analyzed", fieldName.replaceAll("\\.", "..")));
        analyzed = result.size() > 0;
      }

      return analyzed;
    }

  }

}
