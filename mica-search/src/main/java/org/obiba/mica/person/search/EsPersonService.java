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
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.obiba.mica.study.service.IndividualStudyService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EsPersonService extends AbstractIdentifiedDocumentService<Person> {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private PersonRepository personRepository;

  @Inject
  private IndividualStudyService individualStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private NetworkService networkService;

  @Inject
  private MicaConfigService micaConfigService;

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
   * Find the persons that have a membership, in one of the configured roles, of a published study or network the
   * current subject can access. Other persons are filtered out in the search, so the total and the paging only count
   * the published persons.
   */
  public Documents<Person> findPublished(int from, int limit, @Nullable String sort, @Nullable String order,
                                         @Nullable String queryString) {
    List<String> personIds = findPublishedPersonIds();
    if (personIds.isEmpty()) return new Documents<>(0, from, limit);
    return find(from, limit, sort, order, null, queryString, null, null, () -> personIds);
  }

  /**
   * When the search engine fails, the persons are listed from the database, sorted by last name, restricted to the
   * id filter if any. The query is not applied.
   */
  @Override
  protected Documents<Person> findFallback(int from, int limit, @Nullable String order, @Nullable String studyId,
                                           @Nullable Searcher.IdFilter idFilter) {
    Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
    Set<String> ids = idFilter == null ? null : new HashSet<>(idFilter.getValues());
    // ponytail: loads all persons to page them, use a paged query if there are too many
    List<Person> found = personRepository.findAll(Sort.by(direction, "lastName")).stream()
      .filter(p -> ids == null || ids.contains(p.getId()))
      .collect(Collectors.toList());
    Documents<Person> documents = new Documents<>(found.size(), from, limit);
    documents.setDegraded(true);
    found.stream().skip(from).limit(limit).forEach(documents::add);
    return documents;
  }

  private List<String> findPublishedPersonIds() {
    List<String> studyIds = Stream.concat(
        individualStudyService.findPublishedIds().stream()
          .filter(id -> subjectAclService.isAccessible("/individual-study", id)),
        harmonizationStudyService.findPublishedIds().stream()
          .filter(id -> subjectAclService.isAccessible("/harmonization-study", id)))
      .collect(Collectors.toList());
    List<String> networkIds = networkService.findPublishedIds().stream()
      .filter(id -> subjectAclService.isAccessible("/network", id))
      .collect(Collectors.toList());
    if (studyIds.isEmpty() && networkIds.isEmpty()) return List.of();

    List<String> roles = micaConfigService.getConfig().getRoles();
    return personRepository.findIdsByMemberships(studyIds, networkIds, roles).stream()
      .map(Person::getId)
      .collect(Collectors.toList());
  }
}
