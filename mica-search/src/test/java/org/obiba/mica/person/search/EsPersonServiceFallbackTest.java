/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search;

import org.junit.jupiter.api.Test;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsPersonServiceFallbackTest {

  /**
   * The persons are not access filtered: when the search engine fails, they are listed from the database.
   */
  @Test
  public void searchFailureFallsBackOnDatabase() {
    EsPersonService service = new EsPersonService();
    Indexer indexer = mock(Indexer.class);
    when(indexer.hasIndex(Indexer.PERSON_INDEX)).thenReturn(true);
    Searcher searcher = mock(Searcher.class);
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenThrow(new IllegalStateException("search failed"));
    PersonRepository repository = mock(PersonRepository.class);
    when(repository.findAll(Sort.by(Sort.Direction.DESC, "lastName"))).thenReturn(List.of(person("c"), person("b"), person("a")));
    ReflectionTestUtils.setField(service, "indexer", indexer);
    ReflectionTestUtils.setField(service, "searcher", searcher);
    ReflectionTestUtils.setField(service, "personRepository", repository);

    DocumentService.Documents<Person> documents = service.find(1, 5, "lastName", "desc", null, "doe*");

    assertThat(documents.isDegraded()).isTrue();
    assertThat(documents.getTotal()).isEqualTo(3);
    assertThat(documents.getList().stream().map(Person::getLastName)).containsExactly("b", "a");
  }

  private Person person(String lastName) {
    Person person = new Person();
    person.setLastName(lastName);
    return person;
  }
}
