package org.obiba.mica.search.basic;

import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.network.NetworkRepository;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class NetworkSearcher implements DocumentSearcher {

  @Inject
  private NetworkRepository networkRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return DefaultIndexer.DRAFT_NETWORK_INDEX.equals(indexName) && "Network".equals(type);
  }

  @Override
  public Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    // Calculate page number based on offset and limit
    int page = from / limit;
    Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
    // TODO query + term filter
    Collection<String> ids = idFilter == null ? null : idFilter.getValues();
    Pageable pageable = PageRequest.of(page, limit, sortRequest);
    final long total = ids == null ? networkRepository.count() : ids.size();
    final List<Network> networks = (ids == null ? networkRepository.findAll(pageable) : networkRepository.findByIdIn(ids, pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, networks);
  }
}
