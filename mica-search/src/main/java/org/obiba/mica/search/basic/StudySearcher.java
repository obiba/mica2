package org.obiba.mica.search.basic;

import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class StudySearcher implements DocumentSearcher {

  @Inject
  private StudyRepository studyRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return DefaultIndexer.DRAFT_STUDY_INDEX.equals(indexName) && "Study".equals(type);
  }

  @Override
  public Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    // Calculate page number based on offset and limit
    int page = from / limit;
    Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
    // TODO query + term filter
    Collection<String> ids = idFilter == null ? null : idFilter.getValues();
    Pageable pageable = PageRequest.of(page, limit, sortRequest);
    final long total = ids == null ? studyRepository.count() : ids.size();
    final List<Study> studies = (ids == null ? studyRepository.findAll(pageable) : studyRepository.findByIdIn(ids, pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, studies);
  }
}
