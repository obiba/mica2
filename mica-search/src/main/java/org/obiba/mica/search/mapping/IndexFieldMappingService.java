/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
import org.obiba.mica.dataset.event.HarmonizationDatasetIndexedEvent;
import org.obiba.mica.dataset.event.StudyDatasetIndexedEvent;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.spi.search.IndexFieldMapping;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;

/**
 * Service providing Elasticsearch Index Mapping information
 */
@Component
public class IndexFieldMappingService {

  @Inject
  private Indexer indexer;

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
    if (!mappings.containsKey(name))
      mappings.put(name, indexer.getIndexfieldMapping(name, type));
    return mappings.get(name);

  }

}
