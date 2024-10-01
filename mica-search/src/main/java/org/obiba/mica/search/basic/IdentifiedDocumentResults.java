package org.obiba.mica.search.basic;

import org.obiba.mica.spi.search.Identified;
import org.obiba.mica.spi.search.Searcher;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class IdentifiedDocumentResults<T extends Identified> implements Searcher.DocumentResults {
  private final long total;
  private final List<T> documents;

  public IdentifiedDocumentResults(long total, List<T> documents) {
    this.total = total;
    this.documents = documents;
  }

  @Override
  public long getTotal() {
    return total;
  }

  @Override
  public List<Searcher.DocumentResult> getDocuments() {
    return documents.stream().map(IdentifiedDocumentResult::new).collect(Collectors.toList());
  }

  @Override
  public Map<String, Long> getAggregation(String field) {
    return Map.of();
  }

  @Override
  public List<Searcher.DocumentAggregation> getAggregations() {
    return List.of();
  }
}
