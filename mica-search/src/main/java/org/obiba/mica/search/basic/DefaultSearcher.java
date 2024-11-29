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
import org.obiba.mica.spi.search.support.JoinQuery;
import org.obiba.mica.spi.search.support.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class DefaultSearcher implements Searcher {

  @Override
  public JoinQuery makeJoinQuery(String rql) {
    return null;
  }

  @Override
  public Query makeQuery(String rql) {
    return null;
  }

  @Override
  public Query andQuery(Query... queries) {
    return null;
  }

  @Override
  public DocumentResults find(String indexName, String type, String rql, IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults count(String indexName, String type, String rql, IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName) {
    return List.of();
  }

  @Override
  public InputStream getDocumentById(String indexName, String type, String id) {
    return null;
  }

  @Override
  public InputStream getDocumentByClassName(String indexName, String type, Class clazz, String id) {
    return null;
  }

  @Override
  public DocumentResults getDocumentsByClassName(String indexName, String type, Class clazz, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable TermFilter termFilter, @Nullable IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    return new EmptyDocumentResults();
  }

  @Override
  public long countDocumentsWithField(String indexName, String type, String field) {
    return 0;
  }

  @Override
  public DocumentResults query(String indexName, String type, Query query, QueryScope scope, List<String> mandatorySourceFields, Properties aggregationProperties, @Nullable IdFilter idFilter) throws IOException {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults aggregate(String indexName, String type, Query query, Properties aggregationProperties, IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, @Nullable IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, Map<String, Properties> subAggregationProperties, @Nullable IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  @Override
  public Map<Object, Object> harmonizationStatusAggregation(String datasetId, int size, String aggregationFieldName, String statusFieldName) {
    return Map.of();
  }
}
