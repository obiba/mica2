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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EsPersonServiceFallbackTest {

  private static final List<String> ROLES = List.of("contact", "investigator");

  private EsPersonService service;

  private Indexer indexer;

  private Searcher searcher;

  private PersonRepository repository;

  private SubjectAclService subjectAclService;

  private IndividualStudyService individualStudyService;

  private HarmonizationStudyService harmonizationStudyService;

  private NetworkService networkService;

  @BeforeEach
  public void setUp() {
    service = new EsPersonService();
    indexer = mock(Indexer.class);
    when(indexer.hasIndex(Indexer.PERSON_INDEX)).thenReturn(true);
    searcher = mock(Searcher.class);
    repository = mock(PersonRepository.class);
    subjectAclService = mock(SubjectAclService.class);
    individualStudyService = mock(IndividualStudyService.class);
    harmonizationStudyService = mock(HarmonizationStudyService.class);
    networkService = mock(NetworkService.class);
    MicaConfig config = mock(MicaConfig.class);
    when(config.getRoles()).thenReturn(ROLES);
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getConfig()).thenReturn(config);

    ReflectionTestUtils.setField(service, "indexer", indexer);
    ReflectionTestUtils.setField(service, "searcher", searcher);
    ReflectionTestUtils.setField(service, "personRepository", repository);
    ReflectionTestUtils.setField(service, "subjectAclService", subjectAclService);
    ReflectionTestUtils.setField(service, "individualStudyService", individualStudyService);
    ReflectionTestUtils.setField(service, "harmonizationStudyService", harmonizationStudyService);
    ReflectionTestUtils.setField(service, "networkService", networkService);
    ReflectionTestUtils.setField(service, "micaConfigService", micaConfigService);
  }

  /**
   * The draft persons are not access filtered: when the search engine fails, they are all listed from the database.
   */
  @Test
  public void searchFailureFallsBackOnDatabase() {
    searchFails();
    when(repository.findAll(Sort.by(Sort.Direction.DESC, "lastName")))
      .thenReturn(List.of(person("c", "c"), person("b", "b"), person("a", "a")));

    DocumentService.Documents<Person> documents = service.find(1, 5, "lastName", "desc", null, "doe*");

    assertThat(documents.isDegraded()).isTrue();
    assertThat(documents.getTotal()).isEqualTo(3);
    assertThat(documents.getList().stream().map(Person::getLastName)).containsExactly("b", "a");
  }

  @Test
  public void publishedSearchIsRestrictedToPublishedMembers() {
    publishedAndAccessible(List.of("study1", "study2"), List.of("hstudy1"), List.of("network1"));
    when(subjectAclService.isAccessible("/individual-study", "study2")).thenReturn(false);
    when(repository.findIdsByMemberships(any(), any(), any())).thenReturn(List.of(person("p1", "a"), person("p2", "b")));
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(mock(Searcher.DocumentResults.class));

    service.findPublished(0, 10, "lastName", "asc", "*");

    verify(repository).findIdsByMemberships(List.of("study1", "hstudy1"), List.of("network1"), ROLES);
    ArgumentCaptor<Searcher.IdFilter> idFilter = ArgumentCaptor.forClass(Searcher.IdFilter.class);
    verify(searcher).getDocuments(eq(Indexer.PERSON_INDEX), eq(Indexer.PERSON_TYPE), eq(0), eq(10), eq("lastName"),
      eq("asc"), eq("*"), any(), idFilter.capture(), any(), any());
    assertThat(idFilter.getValue().getField()).isEqualTo("id");
    assertThat(idFilter.getValue().getValues()).containsExactly("p1", "p2");
  }

  @Test
  public void publishedSearchWithoutPublishedMembersFindsNothing() {
    publishedAndAccessible(List.of("study1"), List.of(), List.of());
    when(repository.findIdsByMemberships(any(), any(), any())).thenReturn(List.of());

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc", "*");

    assertThat(documents.getTotal()).isZero();
    assertThat(documents.getList()).isEmpty();
    verify(searcher, never()).getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void publishedSearchWithoutPublishedEntitiesFindsNothing() {
    publishedAndAccessible(List.of(), List.of(), List.of());

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc", "*");

    assertThat(documents.getTotal()).isZero();
    verify(repository, never()).findIdsByMemberships(any(), any(), any());
  }

  @Test
  public void publishedSearchFailureFallsBackOnPublishedMembers() {
    publishedAndAccessible(List.of("study1"), List.of(), List.of());
    when(repository.findIdsByMemberships(any(), any(), any())).thenReturn(List.of(person("p1", "a"), person("p3", "c")));
    searchFails();
    when(repository.findAll(Sort.by(Sort.Direction.ASC, "lastName")))
      .thenReturn(List.of(person("p1", "a"), person("p2", "b"), person("p3", "c")));

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc", "*");

    assertThat(documents.isDegraded()).isTrue();
    assertThat(documents.getTotal()).isEqualTo(2);
    assertThat(documents.getList().stream().map(Person::getId)).containsExactly("p1", "p3");
  }

  private void searchFails() {
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenThrow(new IllegalStateException("search failed"));
  }

  private void publishedAndAccessible(List<String> studyIds, List<String> harmonizationStudyIds, List<String> networkIds) {
    when(individualStudyService.findPublishedIds()).thenReturn(studyIds);
    when(harmonizationStudyService.findPublishedIds()).thenReturn(harmonizationStudyIds);
    when(networkService.findPublishedIds()).thenReturn(networkIds);
    when(subjectAclService.isAccessible(anyString(), anyString())).thenReturn(true);
  }

  private Person person(String id, String lastName) {
    Person person = new Person();
    person.setId(id);
    person.setLastName(lastName);
    return person;
  }
}
