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

import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.core.repository.DocumentRepository;
import org.obiba.mica.dataset.HarmonizationDatasetRepository;
import org.obiba.mica.dataset.StudyDatasetRepository;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.search.basic.DocumentSearcher;
import org.obiba.mica.search.basic.IdentifiedDocumentResults;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class DraftDatasetSearcher implements DocumentSearcher {

  private static final Set<String> DATASET_TYPES = Sets.newHashSet(Indexer.DATASET_TYPE, Indexer.STUDY_DATASET_TYPE, Indexer.HARMO_DATASET_TYPE);

  @Inject
  private StudyDatasetRepository studyDatasetRepository;

  @Inject
  private HarmonizationDatasetRepository harmonizationDatasetRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return Indexer.DRAFT_DATASET_INDEX.equals(indexName) && DATASET_TYPES.contains(type);
  }

  @Override
  public Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    // Calculate page number based on offset and limit
    int page = from / limit;
    Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
    // TODO query + term filter
    Collection<String> ids = idFilter == null ? null : idFilter.getValues();
    Pageable pageable = PageRequest.of(page, limit, sortRequest);
    DocumentRepository<? extends Dataset> repository = getRepository(type);
    final long total = ids == null ? repository.count() : ids.size();
    final List<? extends Dataset> studies = (ids == null ? repository.findAll(pageable) : repository.findByIdIn(ids, pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, studies);
  }

  private DocumentRepository<? extends Dataset> getRepository(String type) {
    if (Indexer.HARMO_DATASET_TYPE.equals(type))
      return harmonizationDatasetRepository;
    return studyDatasetRepository;
  }
}
