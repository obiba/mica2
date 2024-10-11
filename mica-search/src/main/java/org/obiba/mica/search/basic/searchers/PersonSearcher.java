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

import jakarta.inject.Inject;
import org.apache.commons.compress.utils.Lists;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.Nullable;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.search.basic.IdentifiedDocumentResults;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersonSearcher extends BaseSearcher {

  private static final Logger log = LoggerFactory.getLogger(PersonSearcher.class);

  private Directory directory;

  @Inject
  private PersonRepository personRepository;

  @Override
  public boolean isFor(String indexName, String type) {
    return Indexer.PERSON_INDEX.equals(indexName);
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
    final long total = ids == null ? personRepository.count() : ids.size();
    final List<Person> persons = (ids == null ? personRepository.findAll(pageable) : personRepository.findByIdIn(ids, pageable)).getContent();
    return new IdentifiedDocumentResults<>(total, persons);
  }
}
