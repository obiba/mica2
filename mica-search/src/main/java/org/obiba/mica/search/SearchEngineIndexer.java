/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import org.obiba.mica.micaConfig.service.PluginsService;
import org.obiba.mica.search.basic.DefaultIndexer;
import org.obiba.mica.spi.search.IndexFieldMapping;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.Map;

@Component
public class SearchEngineIndexer implements Indexer {

  @Inject
  private PluginsService pluginsService;

  private Indexer defaultIndexer;

  private Indexer getIndexer() {
    if (pluginsService.hasSearchEngineService())
      return pluginsService.getSearchEngineService().getIndexer();
    if (defaultIndexer == null) {
      defaultIndexer = new DefaultIndexer();
    }
    return defaultIndexer;
  }

  @Override
  public void index(String indexName, Persistable<String> persistable) {
    getIndexer().index(indexName, persistable);
  }

  @Override
  public void index(String indexName, Persistable<String> persistable, Persistable<String> parent) {
    getIndexer().index(indexName, persistable, parent);
  }

  @Override
  public void index(String indexName, Indexable indexable) {
    getIndexer().index(indexName, indexable);
  }

  @Override
  public void index(String indexName, Indexable indexable, Indexable parent) {
    getIndexer().index(indexName, indexable, parent);
  }

  @Override
  synchronized public void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> persistables) {
    getIndexer().reIndexAllIndexables(indexName, persistables);
  }

  @Override
  synchronized public void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    getIndexer().reindexAll(indexName, persistables);
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    getIndexer().indexAll(indexName, persistables);
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent) {
    getIndexer().indexAll(indexName, persistables, parent);
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    getIndexer().indexAllIndexables(indexName, indexables);
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId) {
    getIndexer().indexAllIndexables(indexName, indexables, parentId);
  }

  @Override
  public void delete(String indexName, Persistable<String> persistable) {
    getIndexer().delete(indexName, persistable);
  }

  @Override
  public void delete(String indexName, Indexable indexable) {
    getIndexer().delete(indexName, indexable);
  }

  @Override
  public void delete(String indexName, String[] types, Map.Entry<String, String> termQuery) {
    getIndexer().delete(indexName, types, termQuery);
  }

  @Override
  public void delete(String indexName, String type, Map.Entry<String, String> termQuery) {
    getIndexer().delete(indexName, type, termQuery);
  }

  @Override
  public boolean hasIndex(String indexName) {
    return getIndexer().hasIndex(indexName);
  }

  @Override
  public void dropIndex(String indexName) {
    getIndexer().dropIndex(indexName);
  }

  @Override
  public IndexFieldMapping getIndexfieldMapping(String indexName, String type) {
    return getIndexer().getIndexfieldMapping(indexName, type);
  }
}
