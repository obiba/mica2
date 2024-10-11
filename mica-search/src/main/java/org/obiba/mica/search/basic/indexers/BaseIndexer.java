/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic.indexers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.Nullable;
import org.obiba.core.util.FileUtil;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.search.basic.DocumentIndexer;
import org.obiba.mica.spi.search.Indexable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Component
public abstract class BaseIndexer<T> implements DocumentIndexer {

  private static final Logger log = LoggerFactory.getLogger(BaseIndexer.class);

  protected Directory directory;

  @Override
  public void index(String indexName, Persistable<String> persistable) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      writer.addDocument(asDocument((T)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void index(String indexName, Persistable<String> persistable, Persistable<String> parent) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      writer.addDocument(asDocument((T)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void index(String indexName, Indexable indexable) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      writer.addDocument(asDocument((T)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void index(String indexName, Indexable indexable, Indexable parent) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      writer.addDocument(asDocument((T)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    cleanDirectory(indexName);
    try (IndexWriter writer = newIndexWriter(indexName)) {
      for (Indexable indexable : indexables)
        writer.addDocument(asDocument((T)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    cleanDirectory(indexName);
    try (IndexWriter writer = newIndexWriter(indexName)) {
      for (Persistable persistable : persistables)
        writer.addDocument(asDocument((T)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      for (Persistable persistable : persistables)
        writer.addDocument(asDocument((T)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      for (Persistable persistable : persistables)
        writer.addDocument(asDocument((T)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      for (Indexable indexable : indexables)
        writer.addDocument(asDocument((T)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      for (Indexable indexable : indexables)
        writer.addDocument(asDocument((T)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Indexer failure", e);
    }
  }

  @Override
  public void delete(String indexName, Persistable<String> persistable) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      // Create a query to match the documents to delete
      Query query = new TermQuery(new Term("id", persistable.getId()));
      // Delete documents that match the query
      writer.deleteDocuments(query);
      writer.commit();
    } catch (Exception e) {
      log.error("Indexer delete failed", e);
    }
  }

  @Override
  public void delete(String indexName, Indexable indexable) {
    try (IndexWriter writer = newIndexWriter(indexName)) {
      // Create a query to match the documents to delete
      Query query = new TermQuery(new Term("id", indexable.getId()));
      // Delete documents that match the query
      writer.deleteDocuments(query);
      writer.commit();
    } catch (Exception e) {
      log.error("Indexer delete failed", e);
    }
  }

  @Override
  public void delete(String indexName, String[] types, Map.Entry<String, String> termQuery) {
    // TODO
  }

  @Override
  public void delete(String indexName, String type, Map.Entry<String, String> termQuery) {
    // TODO
  }

  //
  // Protected methods
  //

  protected abstract Document asDocument(T item);

  protected IndexWriter newIndexWriter(String indexName) {
    try {
      Analyzer analyzer = AnalyzerFactory.newDefaultAnalyzer();
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      return new IndexWriter(getDirectory(indexName), config);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void cleanDirectory(String indexName) {
    try {
      FileUtil.delete(new File(getIndexParentDir(), indexName));
    } catch (IOException e) {
      // ignore
    }
  }

  protected Directory getDirectory(String indexName) {
    if (directory == null) {
      try {
        directory = FSDirectory.open(new File(getIndexParentDir(), indexName).toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return directory;
  }

  protected String addLocalizedString(Document doc, String field, LocalizedString localized) {
    if (localized == null || localized.isEmpty()) return "";
    localized.forEach((key, value) -> {
      doc.add(new TextField(String.format("%s.%s.analyzed", field, key), value, Field.Store.YES));
      doc.add(new StringField(String.format("%s.%s", field, key), value, Field.Store.YES));
    });
    return String.join(" ", localized.values());
  }
}
