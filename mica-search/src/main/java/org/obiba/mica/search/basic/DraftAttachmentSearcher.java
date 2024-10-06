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

import com.google.common.base.Joiner;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class DraftAttachmentSearcher implements DocumentSearcher {

  @Inject
  private AttachmentStateRepository attachmentStateRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return Indexer.ATTACHMENT_DRAFT_INDEX.equals(indexName);
  }

  @Override
  public Searcher.DocumentResults getDocuments(String indexName, String type, int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String queryString, @Nullable Searcher.TermFilter termFilter, @Nullable Searcher.IdFilter idFilter, @Nullable List<String> fields, @Nullable List<String> excludedFields) {
    // Calculate page number based on offset and limit
    int page = from / limit;
    Sort sortRequest = "asc".equalsIgnoreCase(order) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
    // TODO query + term filter
    Collection<String> ids = idFilter == null ? null : idFilter.getValues();
    Pageable pageable = PageRequest.of(page, limit, sortRequest);
    final long total = ids == null ? attachmentStateRepository.count() : ids.size();
    final List<AttachmentState> attachments = (ids == null ? attachmentStateRepository.findAll(pageable) : attachmentStateRepository.findByPath(Joiner.on("|").join(ids), pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, attachments);
  }
}
