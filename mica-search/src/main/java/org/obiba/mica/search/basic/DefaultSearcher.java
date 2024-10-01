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
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.EmptyQuery;
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The default searcher is a proxy class that will redirect queries to the corresponding document index and type.
 */
public class DefaultSearcher implements Searcher {

  private final Set<DocumentSearcher> documentSearchers;

  DefaultSearcher(Set<DocumentSearcher> documentSearchers) {
    this.documentSearchers = documentSearchers;
  }

  @Override
  public JoinQuery makeJoinQuery(String rql) {
    return new EmptyJoinQuery();
  }

  @Override
  public Query makeQuery(String rql) {
    return new EmptyQuery();
  }

  @Override
  public Query andQuery(Query... queries) {
    return new EmptyQuery();
  }

  @Override
  public Map<Object, Object> harmonizationStatusAggregation(String datasetId, int size, String aggregationFieldName, String statusFieldName) {
    return Map.of();
  }

  //
  // Proxy methods
  //

  @Override
  public DocumentResults find(String indexName, String type, String rql, IdFilter idFilter) {
    return getSearcher(indexName, type).find(indexName, type, rql, idFilter);
  }

  @Override
  public DocumentResults count(String indexName, String type, String rql, IdFilter idFilter) {
    return getSearcher(indexName, type).count(indexName, type, rql, idFilter);
  }

  @Override
  public List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName) {
    return getSearcher(indexName, type).suggest(indexName, type, limit, locale, queryString, defaultFieldName);
  }

  @Override
  public InputStream getDocumentById(String indexName, String type, String id) {
    return getSearcher(indexName, type).getDocumentById(indexName, type, id);
  }

  @Override
  public InputStream getDocumentByClassName(String indexName, String type, Class clazz, String id) {
    return getSearcher(indexName, type).getDocumentByClassName(indexName, type, clazz, id);
  }

  @Override
  public DocumentResults getDocumentsByClassName(String indexName, String type, Class clazz, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter) {
    return getSearcher(indexName, type).getDocumentsByClassName(indexName, type, clazz, from, limit, sort, order, queryString, termFilter, idFilter);
  }

  @Override
  public DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    return getSearcher(indexName, type).getDocuments(indexName, type, from, limit, sort, order, queryString, termFilter, idFilter, fields, excludedFields);
  }

  @Override
  public long countDocumentsWithField(String indexName, String type, String field) {
    return getSearcher(indexName, type).countDocumentsWithField(indexName, type, field);
  }

  @Override
  public DocumentResults query(String indexName, String type, Query query, QueryScope scope, List<String> mandatorySourceFields, Properties aggregationProperties, @Nullable IdFilter idFilter) throws IOException {
    return getSearcher(indexName, type).query(indexName, type, query, scope, mandatorySourceFields, aggregationProperties, idFilter);
  }

  @Override
  public DocumentResults aggregate(String indexName, String type, Query query, Properties aggregationProperties, IdFilter idFilter) {
    return getSearcher(indexName, type).aggregate(indexName, type, query, aggregationProperties, idFilter);
  }

  @Override
  public DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, @Nullable IdFilter idFilter) {
    return getSearcher(indexName, type).cover(indexName, type, query, aggregationProperties, idFilter);
  }

  @Override
  public DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, Map<String, Properties> subAggregationProperties, @Nullable IdFilter idFilter) {
    return getSearcher(indexName, type).cover(indexName, type, query, aggregationProperties, subAggregationProperties, idFilter);
  }

  //
  // Private methods
  //

  private DocumentSearcher getSearcher(String indexName, String type) {
    return documentSearchers.stream().filter((searcher) -> searcher.isFor(indexName, type)).findFirst().orElseThrow();
  }

}
