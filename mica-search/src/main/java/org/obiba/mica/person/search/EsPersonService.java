/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;

@Service
public class EsPersonService extends AbstractIdentifiedDocumentService<Person> {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private PersonRepository personRepository;

  @Override
  protected Person processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), Person.class);
  }

  @Override
  protected String getIndexName() {
    return Indexer.PERSON_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.PERSON_TYPE;
  }

  /**
   * The persons are not access filtered: when the search engine fails, they are all listed from the database,
   * sorted by last name. The query is not applied.
   */
  @Override
  protected Documents<Person> findFallback(int from, int limit, @Nullable String order, @Nullable String studyId,
                                           @Nullable Searcher.IdFilter idFilter) {
    Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
    // ponytail: loads all persons to page them, use a paged query if there are too many
    List<Person> found = personRepository.findAll(Sort.by(direction, "lastName"));
    Documents<Person> documents = new Documents<>(found.size(), from, limit);
    documents.setDegraded(true);
    found.stream().skip(from).limit(limit).forEach(documents::add);
    return documents;
  }
}
