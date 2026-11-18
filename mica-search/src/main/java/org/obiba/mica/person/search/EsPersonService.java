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
import com.google.common.base.Strings;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.service.PublishedStudyService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EsPersonService extends AbstractIdentifiedDocumentService<Person> {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private PersonRepository personRepository;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedNetworkService publishedNetworkService;

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
   * current subject can access. The others are filtered out in the search, so the total and the paging only count
   * these persons. When the search engine fails, these persons are listed from the database, without applying the
   * query.
   */
  public Documents<Person> findPublished(int from, int limit, @Nullable String sort, @Nullable String order,
                                         @Nullable String queryString) {
    List<String> studyIds = publishedStudyService.findAccessibleIds();
    List<String> networkIds = publishedNetworkService.findAccessibleIds();
    List<String> roles = micaConfigService.getConfig().getRoles();
    if ((studyIds.isEmpty() && networkIds.isEmpty()) || roles.isEmpty()) return new Documents<>(0, from, limit);

    // ponytail: the memberships are not nested in the index, so the role can match another membership than the
    // published one; findPublishedStudyMembers lists the members of given studies exactly
    List<String> parents = new ArrayList<>();
    if (!studyIds.isEmpty()) parents.add(termsQuery("studyMemberships.parentId", studyIds));
    if (!networkIds.isEmpty()) parents.add(termsQuery("networkMemberships.parentId", networkIds));
    String members = "(" + String.join(" OR ", parents) + ") AND (" + termsQuery("studyMemberships.role", roles)
      + " OR " + termsQuery("networkMemberships.role", roles) + ")";
    String query = Strings.isNullOrEmpty(queryString) ? members : "(" + queryString + ") AND " + members;

    return find(from, limit, sort, order, null, query, null, null, null,
      () -> page(personRepository.findByMemberships(studyIds, networkIds, roles, byLastName(order)), from, limit, true));
  }

  /**
   * Find the persons that have a membership, in one of the configured roles, of the given studies, restricted to the
   * published ones the current subject can access. Listed from the database, sorted by last name.
   */
  public Documents<Person> findPublishedStudyMembers(int from, int limit, @Nullable String order,
                                                     Collection<String> studyIds) {
    List<String> ids = publishedStudyService.findAccessibleIds().stream()
      .filter(studyIds::contains)
      .collect(Collectors.toList());
    List<String> roles = micaConfigService.getConfig().getRoles();
    if (ids.isEmpty() || roles.isEmpty()) return new Documents<>(0, from, limit);
    return page(personRepository.findByMemberships(ids, List.of(), roles, byLastName(order)), from, limit, false);
  }

  /**
   * When the search engine fails, the persons are listed from the database, sorted by last name. The query is not
   * applied.
   */
  @Override
  protected Documents<Person> findFallback(int from, int limit, @Nullable String order, @Nullable String studyId,
                                           @Nullable Searcher.IdFilter idFilter) {
    // ponytail: loads all persons to page them, use a paged query if there are too many
    return page(personRepository.findAll(byLastName(order)), from, limit, true);
  }

  private static Sort byLastName(@Nullable String order) {
    return Sort.by("desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC, "lastName");
  }

  private static Documents<Person> page(List<Person> found, int from, int limit, boolean degraded) {
    Documents<Person> documents = new Documents<>(found.size(), from, limit);
    documents.setDegraded(degraded);
    found.stream().skip(from).limit(limit).forEach(documents::add);
    return documents;
  }

  private static String termsQuery(String field, Collection<String> values) {
    return field + ":(" + values.stream()
      .map(v -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
      .collect(Collectors.joining(" OR ")) + ")";
  }
}
