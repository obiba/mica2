/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import org.springframework.data.domain.Persistable;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Search engine index manager.
 */
public interface Indexer {
  void index(String indexName, Persistable<String> persistable);
  void index(String indexName, Persistable<String> persistable, Persistable<String> parent);
  void index(String indexName, Indexable indexable);
  void index(String indexName, Indexable indexable, Indexable parent);
  void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> persistables);
  void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables);
  void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables);
  void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent);
  void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables);
  void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId);
  void delete(String indexName, Persistable<String> persistable);
  void delete(String indexName, Indexable indexable);
  void delete(String indexName, String[] types, Map.Entry<String, String> termQuery);
  void delete(String indexName, String type, Map.Entry<String, String> termQuery);
  boolean hasIndex(String indexName);
  void dropIndex(String indexName);
}
