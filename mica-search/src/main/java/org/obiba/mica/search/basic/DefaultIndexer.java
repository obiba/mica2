/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic;

import org.jetbrains.annotations.Nullable;
import org.obiba.core.util.FileUtil;
import org.obiba.mica.spi.search.IndexFieldMapping;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.springframework.data.domain.Persistable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class DefaultIndexer implements Indexer {

  private static final String INDEX_PARENT_DIR = System.getProperty("MICA_HOME") + "/work/index";

  private final Set<DocumentIndexer> documentIndexers;

  public DefaultIndexer(Set<DocumentIndexer> documentIndexers) {
    this.documentIndexers = documentIndexers;
  }

  @Override
  public void index(String indexName, Persistable<String> persistable) {
    ensureIndexDir(indexName);
    getIndexer(indexName).index(indexName, persistable);
  }

  @Override
  public void index(String indexName, Persistable<String> persistable, Persistable<String> parent) {
    ensureIndexDir(indexName);
    getIndexer(indexName).index(indexName, persistable, parent);
  }

  @Override
  public void index(String indexName, Indexable indexable) {
    ensureIndexDir(indexName);
    getIndexer(indexName).index(indexName, indexable);
  }

  @Override
  public void index(String indexName, Indexable indexable, Indexable parent) {
    ensureIndexDir(indexName);
    getIndexer(indexName).index(indexName, indexable, parent);
  }

  @Override
  public void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> persistables) {
    ensureIndexDir(indexName);
    getIndexer(indexName).reIndexAllIndexables(indexName, persistables);
  }

  @Override
  public void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    ensureIndexDir(indexName);
    getIndexer(indexName).reindexAll(indexName, persistables);
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    ensureIndexDir(indexName);
    getIndexer(indexName).indexAll(indexName, persistables);
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent) {
    ensureIndexDir(indexName);
    getIndexer(indexName).indexAll(indexName, persistables, parent);
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    ensureIndexDir(indexName);
    getIndexer(indexName).indexAllIndexables(indexName, indexables);
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId) {
    ensureIndexDir(indexName);
    getIndexer(indexName).indexAllIndexables(indexName, indexables, parentId);
  }

  @Override
  public void delete(String indexName, Persistable<String> persistable) {
    ensureIndexDir(indexName);
    getIndexer(indexName).delete(indexName, persistable);
  }

  @Override
  public void delete(String indexName, Indexable indexable) {
    ensureIndexDir(indexName);
    getIndexer(indexName).delete(indexName, indexable);
  }

  @Override
  public void delete(String indexName, String[] types, Map.Entry<String, String> termQuery) {
    ensureIndexDir(indexName);
    getIndexer(indexName).delete(indexName, types, termQuery);
  }

  @Override
  public void delete(String indexName, String type, Map.Entry<String, String> termQuery) {
    ensureIndexDir(indexName);
    getIndexer(indexName).delete(indexName, type, termQuery);
  }

  @Override
  public boolean hasIndex(String indexName) {
    return indexName.endsWith("-draft") || Indexer.PERSON_INDEX.equals(indexName) || getIndexDir(indexName).exists();
  }

  @Override
  public void dropIndex(String indexName) {
    File indexDir = getIndexDir(indexName);
    if (indexDir.exists()) {
      try {
        FileUtil.delete(indexDir);
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @Override
  public IndexFieldMapping getIndexfieldMapping(String indexName, String type) {
    return null;
  }

  //
  // Private methods
  //

  private void ensureIndexDir(String indexName) {
    File indexDir = getIndexDir(indexName);
    if (!indexDir.exists()) {
      indexDir.mkdirs();
    }
  }

  private File getIndexDir(String indexName) {
    return new File(INDEX_PARENT_DIR, indexName);
  }

  private DocumentIndexer getIndexer(String indexName) {
    return documentIndexers.stream().filter((indexer) -> indexer.isFor(indexName)).findFirst().orElseThrow();
  }
}
