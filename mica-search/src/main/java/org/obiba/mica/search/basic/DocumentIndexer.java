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

import org.obiba.mica.spi.search.Indexable;
import org.springframework.data.domain.Persistable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

public interface DocumentIndexer {

  boolean isFor(String indexName);

  void index(String indexName, Persistable<String> persistable);

  void index(String indexName, Persistable<String> persistable, Persistable<String> parent);

  void index(String indexName, Indexable indexable);

  void index(String indexName, Indexable indexable, Indexable parent);

  void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> indexables);

  void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables);

  void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables);

  void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent);

  void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables);

  void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId);

  void delete(String indexName, Persistable<String> persistable);

  void delete(String indexName, Indexable indexable);

  void delete(String indexName, String[] types, Map.Entry<String, String> termQuery);

  void delete(String indexName, String type, Map.Entry<String, String> termQuery);

  default File getIndexParentDir() {
    return new File(System.getProperty("MICA_HOME") + "/work/index");
  }
}
