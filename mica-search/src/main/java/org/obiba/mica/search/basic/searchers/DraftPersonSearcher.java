/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic.searchers;

import com.google.common.base.Strings;
import jakarta.inject.Inject;
import org.apache.commons.compress.utils.Lists;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.search.basic.DocumentSearcher;
import org.obiba.mica.search.basic.IdentifiedDocumentResults;
import org.obiba.mica.search.basic.indexers.AnalyzerFactory;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
public class DraftPersonSearcher implements DocumentSearcher {

  private static final Logger log = LoggerFactory.getLogger(DraftPersonSearcher.class);

  private Directory directory;

  @Inject
  private PersonRepository personRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return Indexer.PERSON_INDEX.equals(indexName);
  }

  @Override
  public Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    // Calculate page number based on offset and limit
    int page = from / limit;
    Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
    // TODO term filter
    List<String> ids = null;
    List<String> foundIds = Strings.isNullOrEmpty(queryString) ? null : searchIds(queryString);
    if (foundIds != null && !foundIds.isEmpty()) {
      ids = Lists.newArrayList(foundIds.iterator());
    }
    Collection<String> filteredIds = idFilter == null ? null : idFilter.getValues();
    if (filteredIds != null && !filteredIds.isEmpty()) {
      if (ids == null) {
        ids = Lists.newArrayList(filteredIds.iterator());
      } else if (foundIds != null && !foundIds.isEmpty()) {
        // intersect
        ids.retainAll(foundIds);
      }
    }
    Pageable pageable = PageRequest.of(page, limit, sortRequest);
    final long total = ids == null ? personRepository.count() : ids.size();
    final List<Person> persons = (ids == null ? personRepository.findAll(pageable) : personRepository.findByIdIn(ids, pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, persons);
  }

  private List<String> searchIds(String queryString) {
    try (IndexReader reader = DirectoryReader.open(getDirectory())) {
      IndexSearcher searcher = new IndexSearcher(reader);
      QueryParser parser = new QueryParser("content", AnalyzerFactory.newPersonsAnalyzer());
      Query query = parser.parse(queryString);
      TopDocs results = searcher.search(query, 10000);
      ScoreDoc[] hits = results.scoreDocs;

      List<String> ids = Lists.newArrayList();
      StoredFields storedFields = reader.storedFields();
      for (ScoreDoc hit : hits) {
        Document doc = storedFields.document(hit.doc);
        log.debug("Document hit: {}", doc);
        String identifier = doc.get("id");
        ids.add(identifier);
      }
      return ids;
    } catch (IOException | ParseException e) {
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
