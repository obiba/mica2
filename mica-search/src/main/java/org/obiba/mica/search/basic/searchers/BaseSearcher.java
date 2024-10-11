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
import org.apache.commons.compress.utils.Lists;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.search.basic.DocumentSearcher;
import org.obiba.mica.search.basic.indexers.AnalyzerFactory;
import org.obiba.mica.spi.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public abstract class BaseSearcher implements DocumentSearcher {

  private static final Logger log = LoggerFactory.getLogger(BaseSearcher.class);

  private Directory directory;

  protected List<String> searchIds(String indexName, String type, String queryString, @Nullable Searcher.IdFilter idFilter) {
    List<String> ids = null;
    if (idFilter != null && idFilter.getValues().isEmpty()) {
      return Lists.newArrayList();
    }
    List<String> foundIds = Strings.isNullOrEmpty(queryString) ? null : searchIds(indexName, type, queryString);
    if (foundIds != null) {
      ids = Lists.newArrayList(foundIds.iterator());
    }
    Collection<String> filteredIds = idFilter == null ? null : idFilter.getValues();
    if (filteredIds != null && !filteredIds.isEmpty()) {
      if (ids == null) {
        ids = Lists.newArrayList(filteredIds.iterator());
      } else {
        // intersect
        ids.retainAll(filteredIds);
      }
    }
    return ids;
  }

  protected List<String> searchIds(String indexName, String type, String queryString) {
    try (IndexReader reader = DirectoryReader.open(getDirectory(indexName))) {
      IndexSearcher searcher = new IndexSearcher(reader);
      QueryParser parser = new QueryParser("_content", AnalyzerFactory.newDefaultAnalyzer());
      Query parsedQuery = parser.parse(queryString);
      Query query = parsedQuery;
      if (!Strings.isNullOrEmpty(type)) {
        Query termQuery = new TermQuery(new Term("_class", type));
        query = new BooleanQuery.Builder()
          .add(termQuery, BooleanClause.Occur.MUST)
          .add(parsedQuery, BooleanClause.Occur.MUST)
          .build();
      }

      TopDocs results = searcher.search(query, Integer.MAX_VALUE);
      ScoreDoc[] hits = results.scoreDocs;

      List<String> ids = Lists.newArrayList();
      StoredFields storedFields = reader.storedFields();
      for (ScoreDoc hit : hits) {
        Document doc = storedFields.document(hit.doc);
        log.debug("Document hit: {} {} {}", indexName, type, doc);
        String identifier = doc.get("_id");
        ids.add(identifier);
      }
      return ids;
    } catch (IndexNotFoundException e) {
      log.warn("Index not found: {}", indexName);
      return Lists.newArrayList();
    } catch (IOException | ParseException e) {
      throw new RuntimeException(e);
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
}
