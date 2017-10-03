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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Defines the search operations that can be performed within the search engine.
 */
public interface Searcher {

  // TODO isolate elasticsearch as search functionalities
  SearchRequestBuilder prepareSearch(String... indices);

  /**
   * Parse the RQL string query to make a {@link JoinQuery} that will be passed to the query execution requests.
   *
   * @param rql
   * @return
   */
  JoinQuery makeJoinQuery(String rql);

  /**
   * Parse the RQL string query to make a {@link Query} that will be passed to the query execution requests.
   *
   * @param rql
   * @return
   */
  Query makeQuery(String rql);

  DocumentResults find(String indexName, String type, String rql);

  /**
   * Get field suggestions from query.
   *
   * @param indexName
   * @param type
   * @param limit
   * @param locale
   * @param queryString
   * @param defaultFieldName
   * @return
   */
  List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName);

  /**
   * Get a document by it ID.
   *
   * @param indexName
   * @param type
   * @param id
   * @return null if not found
   */
  InputStream getDocumentById(String indexName, String type, String id);

  /**
   * Get a document by its ID and class name (className field).
   *
   * @param indexName
   * @param type
   * @param clazz
   * @param id
   * @return null if not found
   */
  InputStream getDocumentByClassName(String indexName, String type, Class clazz, String id);

  /**
   * Get the documents matching a class (className field) and some query string and filters.
   *
   * @param indexName
   * @param type
   * @param clazz
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param queryString
   * @param termFilter
   * @param idFilter
   * @return
   */
  DocumentResults getDocumentsByClassName(String indexName, String type, Class clazz, int from, int limit,
                                          @Nullable String sort, @Nullable String order, @Nullable String queryString,
                                          @Nullable TermFilter termFilter, @Nullable IdFilter idFilter);

  /**
   * Get documents with field specifications.
   *
   * @param indexName
   * @param type
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param queryString
   * @param termFilter
   * @param idFilter
   * @param fields
   * @param excludedFields
   * @return
   */
  DocumentResults getDocuments(String indexName, String type, int from, int limit,
                               @Nullable String sort, @Nullable String order, @Nullable String queryString,
                               @Nullable TermFilter termFilter, @Nullable IdFilter idFilter,
                               @Nullable List<String> fields, @Nullable List<String> excludedFields);

  /**
   * Count the documents having a value for the given field.
   *
   * @param indexName
   * @param type
   * @param field
   * @return
   */
  long countDocumentsWithField(String indexName, String type, String field);

  /**
   * Read the found documents.
   */
  interface DocumentResults {
    /**
     * Total number of matching documents.
     *
     * @return
     */
    long getTotal();

    /**
     * Streams to read the found documents.
     *
     * @return
     */
    List<DocumentResult> getDocuments();
  }

  /**
   * Read the found document, extracted from search hit.
   */
  interface DocumentResult {

    /**
     * Identifier.
     *
     * @return
     */
    String getId();

    /**
     * Source content.
     *
     * @return
     */
    InputStream getSourceInputStream();

    /**
     * Value of the field className if any.
     *
     * @return
     */
    String getClassName();
  }

  /**
   * Filter documents by field value.
   */
  interface TermFilter {
    String getField();

    String getValue();
  }

  /**
   * Filter documents by field possible values.
   */
  interface TermsFilter {
    String getField();

    Collection<String> getValues();
  }

  /**
   * Provides the list of document IDs that are accessible by the current subject.
   */
  interface IdFilter extends TermsFilter {

    default String getField() {
      return "id";
    }

  }

  /**
   * Provides the list of file paths that can or cannot be accessed by the current subject.
   */
  interface PathFilter extends IdFilter {

    default String getField() {
      return "path";
    }

    Collection<String> getExcludedValues();

  }
}
