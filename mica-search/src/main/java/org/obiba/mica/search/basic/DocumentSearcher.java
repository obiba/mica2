package org.obiba.mica.search.basic;

import org.jetbrains.annotations.Nullable;
import org.obiba.mica.spi.search.QueryScope;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.spi.search.support.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface DocumentSearcher {

  boolean isFor(String indexName, String type);

  default Searcher.DocumentResults find(String indexName, String type, String rql, Searcher.IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  default Searcher.DocumentResults count(String indexName, String type, String rql, Searcher.IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  default List<String> suggest(String indexName, String type, int limit, String locale, String queryString, String defaultFieldName) {
    return List.of();
  }

  default InputStream getDocumentById(String indexName, String type, String id) {
    return null;
  }

  default InputStream getDocumentByClassName(String indexName, String type, Class clazz, String id) {
    return null;
  }

  default Searcher.DocumentResults getDocumentsByClassName(String indexName, String type, Class clazz, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  default Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    return new EmptyDocumentResults();
  }

  default long countDocumentsWithField(String indexName, String type, String field) {
    return 0;
  }

  default Searcher.DocumentResults query(String indexName, String type, Query query, QueryScope scope, List<String> mandatorySourceFields, Properties aggregationProperties, @Nullable Searcher.IdFilter idFilter) throws IOException {
    return new EmptyDocumentResults();
  }

  default Searcher.DocumentResults aggregate(String indexName, String type, Query query, Properties aggregationProperties, Searcher.IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  default Searcher.DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, @Nullable Searcher.IdFilter idFilter) {
    return new EmptyDocumentResults();
  }

  default Searcher.DocumentResults cover(String indexName, String type, Query query, Properties aggregationProperties, Map<String, Properties> subAggregationProperties, @Nullable Searcher.IdFilter idFilter) {
    return new EmptyDocumentResults();
  }


}
