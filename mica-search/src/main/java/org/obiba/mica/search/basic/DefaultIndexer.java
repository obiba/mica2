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
import org.obiba.mica.spi.search.IndexFieldMapping;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.springframework.data.domain.Persistable;

import java.util.Map;

public class DefaultIndexer implements Indexer {
  @Override
  public void index(String indexName, Persistable<String> persistable) {

  }

  @Override
  public void index(String indexName, Persistable<String> persistable, Persistable<String> parent) {

  }

  @Override
  public void index(String indexName, Indexable indexable) {

  }

  @Override
  public void index(String indexName, Indexable indexable, Indexable parent) {

  }

  @Override
  public void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> persistables) {

  }

  @Override
  public void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {

  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {

  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent) {

  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {

  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId) {

  }

  @Override
  public void delete(String indexName, Persistable<String> persistable) {

  }

  @Override
  public void delete(String indexName, Indexable indexable) {

  }

  @Override
  public void delete(String indexName, String[] types, Map.Entry<String, String> termQuery) {

  }

  @Override
  public void delete(String indexName, String type, Map.Entry<String, String> termQuery) {

  }

  @Override
  public boolean hasIndex(String indexName) {
    return indexName.endsWith("-draft");
  }

  @Override
  public void dropIndex(String indexName) {

  }

  @Override
  public IndexFieldMapping getIndexfieldMapping(String indexName, String type) {
    return null;
  }
}
