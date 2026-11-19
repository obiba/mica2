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
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EsPersonServiceFallbackTest {

  private static final List<String> ROLES = List.of("contact", "investigator");

  private static final Sort BY_LAST_NAME = Sort.by(Sort.Direction.ASC, "lastName");

  private EsPersonService service;

  private Searcher searcher;

  private PersonRepository repository;

  private PublishedStudyService publishedStudyService;

  private PublishedNetworkService publishedNetworkService;

  @BeforeEach
  public void setUp() {
    service = new EsPersonService();
    Indexer indexer = mock(Indexer.class);
    when(indexer.hasIndex(Indexer.PERSON_INDEX)).thenReturn(true);
    searcher = mock(Searcher.class);
    repository = mock(PersonRepository.class);
    publishedStudyService = mock(PublishedStudyService.class);
    publishedNetworkService = mock(PublishedNetworkService.class);
    MicaConfig config = mock(MicaConfig.class);
    when(config.getRoles()).thenReturn(ROLES);
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getConfig()).thenReturn(config);

    ReflectionTestUtils.setField(service, "indexer", indexer);
    ReflectionTestUtils.setField(service, "searcher", searcher);
    ReflectionTestUtils.setField(service, "personRepository", repository);
    ReflectionTestUtils.setField(service, "publishedStudyService", publishedStudyService);
    ReflectionTestUtils.setField(service, "publishedNetworkService", publishedNetworkService);
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
    published(List.of("study1", "hstudy1"), List.of("network1"));
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(mock(Searcher.DocumentResults.class));

    service.findPublished(0, 10, "lastName", "asc", "doe*", List.of("p1"));

    verify(searcher).getDocuments(eq(Indexer.PERSON_INDEX), eq(Indexer.PERSON_TYPE), eq(0), eq(10), eq("lastName"),
      eq("asc"), eq("(" + EsPersonService.publicTextQuery("doe*") + ")"
        + " AND (studyMemberships.parentId:(\"study1\" OR \"hstudy1\") OR networkMemberships.parentId:(\"network1\"))"
        + " AND (studyMemberships.role:(\"contact\" OR \"investigator\") OR networkMemberships.role:(\"contact\" OR \"investigator\")) AND NOT id:(\"p1\")"),
      any(), any(), any(), any());
    verify(repository, never()).findByMemberships(any(), any(), any(), any());
  }

  /**
   * The free text cannot query the hidden fields: the field syntax is escaped and searched on the public fields.
   */
  @Test
  public void publishedSearchTextOnlyQueriesPublicFields() {
    String query = EsPersonService.publicTextQuery("lastModifiedBy:jdoe OR (x)");
    assertThat(query).contains("lastName.analyzed:lastModifiedBy\\:jdoe", "institution.address.city.\\*:\\(x\\)", ") OR (")
      .doesNotContain(" lastModifiedBy:");
  }

  /**
   * The keyword mapped fields are case-sensitive and match the whole value: the text is searched on their analyzed
   * sub-field, so that doe* finds Doe.
   */
  @Test
  public void publishedSearchTextQueriesKeywordFieldsOnAnalyzedSubField() {
    String query = EsPersonService.publicTextQuery("doe*");
    assertThat(query).contains("firstName.analyzed:doe*", "lastName.analyzed:doe*", "email.analyzed:doe*")
      .doesNotContainPattern("(^|[( ])(firstName|lastName|email):");
  }

  /**
   * The public search is sorted on the keyword fields, not on their analyzed sub-field.
   */
  @Test
  public void publishedSearchSortsOnKeywordFields() {
    assertThat(EsPersonService.isPublicField("firstName")).isTrue();
    assertThat(EsPersonService.isPublicField("lastName")).isTrue();
    assertThat(EsPersonService.isPublicField("email")).isTrue();
    assertThat(EsPersonService.isPublicField("lastName.analyzed")).isFalse();
  }

  /**
   * The boolean operators that would not parse are dropped, instead of failing the search.
   */
  @Test
  public void publishedSearchTextDropsDanglingOperators() {
    assertThat(EsPersonService.publicTextQuery("doe AND")).isEqualTo(EsPersonService.publicTextQuery("doe"));
    assertThat(EsPersonService.publicTextQuery("OR")).isEmpty();
    assertThat(EsPersonService.publicTextQuery("a AND OR NOT NOT b"))
      .isEqualTo(EsPersonService.publicTextQuery("a") + " AND NOT " + EsPersonService.publicTextQuery("b"));
  }

  @Test
  public void publishedSearchIsOnlySortedOnPublicFields() {
    published(List.of("study1"), List.of());
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(mock(Searcher.DocumentResults.class));

    service.findPublished(0, 10, "studyMemberships.role", "asc", null, null);
    service.findPublished(0, 10, "institution.name.en", "asc", null, null);

    verify(searcher).getDocuments(any(), any(), eq(0), eq(10), eq("lastName"), any(), any(), any(), any(), any(), any());
    verify(searcher).getDocuments(any(), any(), eq(0), eq(10), eq("institution.name.en"), any(), any(), any(), any(), any(), any());
  }

  /**
   * A members query on an unpublished study does not reveal its published members.
   */
  @Test
  public void publishedMembersQueryIsRestrictedToPublishedStudies() {
    published(List.of("study1"), List.of("network1"));

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc",
      "studyMemberships.parentId:(unpublished)", null);

    assertThat(documents.getTotal()).isZero();
    verify(repository, never()).findByMemberships(any(), any(), any(), any());
    verify(searcher, never()).getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * The members query lists the members of the published studies exactly, from the database.
   */
  @Test
  public void publishedMembersQueryOnPublishedStudy() {
    published(List.of("study1", "study2"), List.of("network1"));
    when(repository.findByMemberships(List.of("study1"), List.of(), ROLES, BY_LAST_NAME))
      .thenReturn(List.of(person("p1", "a"), person("p2", "b")));

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc",
      "studyMemberships.parentId:(study1 or unpublished)", List.of("p2"));

    assertThat(documents.isDegraded()).isFalse();
    assertThat(documents.getList().stream().map(Person::getId)).containsExactly("p1");
    verify(searcher, never()).getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * Another memberships query is searched as text on the public fields.
   */
  @Test
  public void publishedOtherMembershipsQueryIsSearchedAsText() {
    published(List.of("study1"), List.of());
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(mock(Searcher.DocumentResults.class));

    service.findPublished(0, 10, "lastName", "asc", "studyMemberships.role:b*", null);

    verify(searcher).getDocuments(any(), any(), eq(0), eq(10), any(), any(),
      org.mockito.ArgumentMatchers.startsWith("((title:studyMemberships.role\\:b* OR"), any(), any(), any(), any());
  }

  @Test
  public void publishedSearchWithoutPublishedEntitiesFindsNothing() {
    published(List.of(), List.of());

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc", "*", null);

    assertThat(documents.getTotal()).isZero();
    verify(searcher, never()).getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void publishedSearchFailureFallsBackOnPublishedMembers() {
    published(List.of("study1"), List.of("network1"));
    searchFails();
    when(repository.findByMemberships(List.of("study1"), List.of("network1"), ROLES, BY_LAST_NAME))
      .thenReturn(List.of(person("p1", "a"), person("p3", "c")));

    DocumentService.Documents<Person> documents = service.findPublished(0, 10, "lastName", "asc", "*", List.of("p3"));

    assertThat(documents.isDegraded()).isTrue();
    assertThat(documents.getTotal()).isEqualTo(1);
    assertThat(documents.getList().stream().map(Person::getId)).containsExactly("p1");
    verify(repository, never()).findAll(any(Sort.class));
  }

  @Test
  public void studyMembersAreRestrictedToAccessiblePublishedStudies() {
    published(List.of("study1", "study2"), List.of());
    when(repository.findByMemberships(List.of("study1"), List.of(), ROLES, BY_LAST_NAME))
      .thenReturn(List.of(person("p1", "a"), person("p2", "b"), person("p3", "c")));

    DocumentService.Documents<Person> documents = service.findPublishedStudyMembers(1, 5, "asc", List.of("study1", "unpublished"));

    assertThat(documents.isDegraded()).isFalse();
    assertThat(documents.getTotal()).isEqualTo(3);
    assertThat(documents.getList().stream().map(Person::getId)).containsExactly("p2", "p3");
  }

  @Test
  public void membersOfNoAccessibleStudyFindsNothing() {
    published(List.of("study1"), List.of());

    DocumentService.Documents<Person> documents = service.findPublishedStudyMembers(0, 10, "asc", List.of("unpublished"));

    assertThat(documents.getTotal()).isZero();
    verify(repository, never()).findByMemberships(any(), any(), any(), any());
  }

  private void searchFails() {
    when(searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenThrow(new IllegalStateException("search failed"));
  }

  private void published(List<String> studyIds, List<String> networkIds) {
    when(publishedStudyService.findAccessibleIds()).thenReturn(studyIds);
    when(publishedNetworkService.findAccessibleIds()).thenReturn(networkIds);
  }

  private Person person(String id, String lastName) {
    Person person = new Person();
    person.setId(id);
    person.setLastName(lastName);
    return person;
  }
}
