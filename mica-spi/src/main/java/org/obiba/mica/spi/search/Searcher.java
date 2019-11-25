/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Defines the search operations that can be performed within the search engine.
 */
public interface Searcher {

  String AGG_TOTAL_COUNT = "totalCount";

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

  /**
   * Make a query from several queries, joined with and().
   *
   * @param queries
   * @return
   */
  Query andQuery(Query... queries);

  /**
   * Search for documents matching the RQL query. The RQL query can include limit() and sort() statements.
   */
  DocumentResults find(String indexName, String type, String rql, IdFilter idFilter);

  default DocumentResults find(String indexName, String type, String rql) {
    return find(indexName, type, rql, null);
  }

  /**
   * Count documents matching the RQL query. The RQL query can include an aggregate() statement.
   *
   * @param indexName
   * @param type
   * @param rql
   * @param idFilter
   * @return
   */
  DocumentResults count(String indexName, String type, String rql, IdFilter idFilter);

  default DocumentResults count(String indexName, String type, String rql) {
    return count(indexName, type, rql, null);
  }

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
   * Query documents.
   *
   * @param indexName
   * @param type
   * @param query
   * @param scope
   * @param mandatorySourceFields
   * @param aggregationProperties
   * @param idFilter
   * @return
   * @throws IOException
   */
  DocumentResults query(String indexName, String type, Query query, QueryScope scope, List<String> mandatorySourceFields, Properties aggregationProperties, @Nullable IdFilter idFilter) throws IOException;

  /**
   * Get document aggregations from provided properties.
   *
   * @param indexName
   * @param type
   * @param query
   * @param aggregationProperties
   * @param idFilter
   * @return
   */
  DocumentResults aggregate(String indexName, String type, Query query, Properties aggregationProperties, IdFilter idFilter);

  /**
   * Get document nested aggregations from provided properties.
   *
   * @param indexName
   * @param type
   * @param query
   * @param aggregationProperties
   * @param idFilter
   * @return
   * @throws IOException
   */
  DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, @Nullable IdFilter idFilter);

  DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, Map<String, Properties> subAggregationProperties, @Nullable IdFilter idFilter);


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

    /**
     * Get the aggregation counts (count of hits by observed term value) for the field.
     *
     * @param field
     * @return
     */
    Map<String, Long> getAggregation(String field);

    List<DocumentAggregation> getAggregations();
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
     * Whether the document source can be extracted.
     *
     * @return
     */
    boolean hasSource();

    Map<String, Object> getSource();

    /**
     * Source content stream.
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

  interface DocumentAggregation {
    String getName();

    String getType();

    DocumentStatsAggregation asStats();

    DocumentTermsAggregation asTerms();

    DocumentRangeAggregation asRange();

    DocumentGlobalAggregation asGlobal();
  }

  interface DocumentStatsAggregation {

    /**
     * @return The number of values that were aggregated.
     */
    long getCount();

    /**
     * @return The minimum value of all aggregated values.
     */
    double getMin();

    /**
     * @return The maximum value of all aggregated values.
     */
    double getMax();

    /**
     * @return The avg value over all aggregated values.
     */
    double getAvg();

    /**
     * @return The sum of aggregated values.
     */
    double getSum();
  }

  interface DocumentRangeAggregation {
    List<DocumentRangeBucket> getBuckets();
  }

  interface DocumentRangeBucket {

    /**
     * @return The key associated with the bucket as a string
     */
    String getKeyAsString();

    /**
     * @return The number of documents that fall within this bucket
     */
    long getDocCount();

    /**
     * @return The lower bound of the range
     */
    Double getFrom();

    /**
     * @return The upper bound of the range (excluding)
     */
    Double getTo();

    /**
     * @return The sub aggregations
     */
    List<DocumentAggregation> getAggregations();
  }

  interface DocumentTermsAggregation {
    List<DocumentTermsBucket> getBuckets();
  }

  interface DocumentTermsBucket {

    /**
     * @return The key associated with the bucket as a string
     */
    String getKeyAsString();

    /**
     * @return The number of documents that fall within this bucket
     */
    long getDocCount();

    /**
     * @return The sub aggregations
     */
    List<DocumentAggregation> getAggregations();
  }

  interface DocumentGlobalAggregation {

    /**
     * @return The number of documents in this bucket
     */
    long getDocCount();

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
