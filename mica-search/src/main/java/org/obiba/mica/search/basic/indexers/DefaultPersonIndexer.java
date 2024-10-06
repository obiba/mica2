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

import com.google.common.base.Strings;
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
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.search.basic.DocumentIndexer;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Component
public class DefaultPersonIndexer implements DocumentIndexer {

  private static final Logger log = LoggerFactory.getLogger(DefaultPersonIndexer.class);

  private Directory directory;

  @Override
  public boolean isFor(String indexName) {
    return Indexer.PERSON_INDEX.equals(indexName);
  }

  @Override
  public void index(String indexName, Persistable<String> persistable) {
    try (IndexWriter writer = newIndexWriter()) {
      writer.addDocument(asDocument((Person)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void index(String indexName, Persistable<String> persistable, Persistable<String> parent) {
    try (IndexWriter writer = newIndexWriter()) {
      writer.addDocument(asDocument((Person)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void index(String indexName, Indexable indexable) {
    try (IndexWriter writer = newIndexWriter()) {
      writer.addDocument(asDocument((Person)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void index(String indexName, Indexable indexable, Indexable parent) {
    try (IndexWriter writer = newIndexWriter()) {
      writer.addDocument(asDocument((Person)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void reIndexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    try (IndexWriter writer = newIndexWriter()) {
      for (Indexable indexable : indexables)
        writer.addDocument(asDocument((Person)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void reindexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    try (IndexWriter writer = newIndexWriter()) {
      for (Persistable persistable : persistables)
        writer.addDocument(asDocument((Person)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables) {
    try (IndexWriter writer = newIndexWriter()) {
      for (Persistable persistable : persistables)
        writer.addDocument(asDocument((Person)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void indexAll(String indexName, Iterable<? extends Persistable<String>> persistables, Persistable<String> parent) {
    try (IndexWriter writer = newIndexWriter()) {
      for (Persistable persistable : persistables)
        writer.addDocument(asDocument((Person)persistable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables) {
    try (IndexWriter writer = newIndexWriter()) {
      for (Indexable indexable : indexables)
        writer.addDocument(asDocument((Person)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void indexAllIndexables(String indexName, Iterable<? extends Indexable> indexables, @Nullable String parentId) {
    try (IndexWriter writer = newIndexWriter()) {
      for (Indexable indexable : indexables)
        writer.addDocument(asDocument((Person)indexable));
      writer.commit();
    } catch (IOException e) {
      log.error("Person indexer failure", e);
    }
  }

  @Override
  public void delete(String indexName, Persistable<String> persistable) {
    try (IndexWriter writer = newIndexWriter()) {
      // Create a query to match the documents to delete
      Query query = new TermQuery(new Term("id", persistable.getId()));
      // Delete documents that match the query
      writer.deleteDocuments(query);
      writer.commit();
    } catch (Exception e) {
      log.error("Person indexer delete failed", e);
    }
  }

  @Override
  public void delete(String indexName, Indexable indexable) {
    try (IndexWriter writer = newIndexWriter()) {
      // Create a query to match the documents to delete
      Query query = new TermQuery(new Term("id", indexable.getId()));
      // Delete documents that match the query
      writer.deleteDocuments(query);
      writer.commit();
    } catch (Exception e) {
      log.error("Person indexer delete failed", e);
    }
  }

  @Override
  public void delete(String indexName, String[] types, Map.Entry<String, String> termQuery) {

  }

  @Override
  public void delete(String indexName, String type, Map.Entry<String, String> termQuery) {

  }

  //
  // Private methods
  //

  private Document asDocument(Person person) {
    Document doc = new Document();
    doc.add(new StringField("id", person.getId(), Field.Store.YES));

    if (!Strings.isNullOrEmpty(person.getFirstName()))
      doc.add(new TextField("first-name", person.getFirstName(), Field.Store.YES));
    if (!Strings.isNullOrEmpty(person.getLastName()))
      doc.add(new TextField("last-name", person.getLastName(), Field.Store.YES));
    if (!Strings.isNullOrEmpty(person.getEmail()))
      doc.add(new TextField("email", person.getEmail(), Field.Store.YES));

    String content = String.format("%s %s %s", person.getFirstName(), person.getLastName(), person.getEmail());

    doc.add(new TextField("content", content, Field.Store.NO));

    return doc;
  }

  private IndexWriter newIndexWriter() {
    try {
      Analyzer analyzer = AnalyzerFactory.newPersonsAnalyzer();
      IndexWriterConfig config = new IndexWriterConfig(analyzer);
      return new IndexWriter(getDirectory(), config);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Directory getDirectory() {
    if (directory == null) {
      try {
        directory = FSDirectory.open(new File(getIndexParentDir(), Indexer.PERSON_INDEX).toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return directory;
  }
}
