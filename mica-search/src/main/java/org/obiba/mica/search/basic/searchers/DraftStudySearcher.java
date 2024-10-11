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
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.core.repository.DocumentRepository;
import org.obiba.mica.search.basic.IdentifiedDocumentResults;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.HarmonizationStudyRepository;
import org.obiba.mica.study.StudyRepository;
import org.obiba.mica.study.domain.BaseStudy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DraftStudySearcher extends BaseSearcher {

  private static final Set<String> STUDY_TYPES = Sets.newHashSet(Indexer.STUDY_TYPE, Indexer.HARMO_STUDY_TYPE);

  @Inject
  private StudyRepository studyRepository;

  @Inject
  private HarmonizationStudyRepository harmonizationStudyRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return Indexer.DRAFT_STUDY_INDEX.equals(indexName) && STUDY_TYPES.contains(type);
  }

  @Override
  public Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    // TODO term filter
    List<String> ids = searchIds(indexName, type, queryString, idFilter);
    if (ids != null && ids.isEmpty()) {
      return new IdentifiedDocumentResults<>(0, Lists.newArrayList());
    }
    // Calculate page number based on offset and limit
    int page = from / limit;
    Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
    Pageable pageable = PageRequest.of(page, limit, sortRequest);
    DocumentRepository<? extends BaseStudy> repository = getRepository(type);
    final long total = ids == null ? repository.count() : ids.size();
    final List<? extends BaseStudy> studies = (ids == null ? repository.findAll(pageable) : repository.findByIdIn(ids, pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, studies);
  }

  private DocumentRepository<? extends BaseStudy> getRepository(String type) {
    if (Indexer.STUDY_TYPE.equals(type))
      return studyRepository;
    return harmonizationStudyRepository;
  }
}
