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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EsPersonService extends AbstractIdentifiedDocumentService<Person> {

  /**
   * The person fields the public free text search is applied on: the ones the public endpoint returns.
   */
  private static final List<String> PUBLIC_QUERY_FIELDS = List.of("title", "firstName", "lastName", "academicLevel",
    "email", "phone", "institution.name.\\*", "institution.department.\\*", "institution.address.street.\\*",
    "institution.address.city.\\*", "institution.address.zip", "institution.address.state",
    "institution.address.countryIso");

  /**
   * The members query sent by the former study and network page templates.
   */
  private static final Pattern STUDY_MEMBERS_QUERY = Pattern.compile("\\s*studyMemberships\\.parentId:\\(([^()]+)\\)\\s*");

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
   *
   * The query is either the members query of studies, <code>studyMemberships.parentId:(id1 OR id2)</code>, restricted
   * to the accessible published ones, or a free text searched on the returned person fields only, so that the hidden
   * memberships and fields cannot be queried.
   */
  public Documents<Person> findPublished(int from, int limit, @Nullable String sort, @Nullable String order,
                                         @Nullable String queryString, @Nullable Collection<String> excludes) {
    Matcher membersQuery = Strings.isNullOrEmpty(queryString) ? null : STUDY_MEMBERS_QUERY.matcher(queryString);
    if (membersQuery != null && membersQuery.matches()) {
      List<String> queried = Arrays.stream(membersQuery.group(1).trim().split("\\s+"))
        .filter(id -> !id.equalsIgnoreCase("OR"))
        .map(id -> id.replaceAll("^\"|\"$", "")).collect(Collectors.toList());
      return studyMembers(from, limit, order, queried, excludes);
    }

    List<String> studyIds = publishedStudyService.findAccessibleIds();
    List<String> networkIds = publishedNetworkService.findAccessibleIds();
    List<String> roles = micaConfigService.getConfig().getRoles();
    if ((studyIds.isEmpty() && networkIds.isEmpty()) || roles.isEmpty()) return new Documents<>(0, from, limit);

    // ponytail: the memberships are not nested in the index, so the role can match another membership than the
    // published one; the members query is listed exactly from the database
    List<String> parents = new ArrayList<>();
    if (!studyIds.isEmpty()) parents.add(termsQuery("studyMemberships.parentId", studyIds));
    if (!networkIds.isEmpty()) parents.add(termsQuery("networkMemberships.parentId", networkIds));
    String query = "(" + String.join(" OR ", parents) + ") AND (" + termsQuery("studyMemberships.role", roles)
      + " OR " + termsQuery("networkMemberships.role", roles) + ")";
    String text = Strings.isNullOrEmpty(queryString) ? "" : publicTextQuery(queryString);
    if (!text.isEmpty()) query = "(" + text + ") AND " + query;
    if (excludes != null && !excludes.isEmpty()) query += " AND NOT " + termsQuery("id", excludes);

    return find(from, limit, isPublicField(sort) ? sort : "lastName", order, null, query, null, null, null,
      () -> page(personRepository.findByMemberships(studyIds, networkIds, roles, byLastName(order)), excludes, from, limit, true));
  }

  /**
   * Each term of the text is searched on the public person fields. The query syntax characters are escaped, except
   * the wildcards, so that no other field can be queried. The boolean operators that would not parse are dropped.
   */
  static String publicTextQuery(String text) {
    List<String> terms = new ArrayList<>();
    for (String term : text.trim().split("\\s+")) {
      String last = terms.isEmpty() ? null : terms.get(terms.size() - 1);
      if (term.equals("AND") || term.equals("OR")) {
        if (last != null && !isOperator(last)) terms.add(term);
      } else if (term.equals("NOT")) {
        if (!"NOT".equals(last)) terms.add(term);
      } else if (!term.isEmpty()) {
        String escaped = term.replaceAll("([+\\-=&|><!(){}\\[\\]^\"~:\\\\/])", "\\\\$1");
        terms.add("(" + PUBLIC_QUERY_FIELDS.stream().map(f -> f + ":" + escaped).collect(Collectors.joining(" OR ")) + ")");
      }
    }
    while (!terms.isEmpty() && isOperator(terms.get(terms.size() - 1))) terms.remove(terms.size() - 1);
    return String.join(" ", terms);
  }

  private static boolean isOperator(String term) {
    return term.equals("AND") || term.equals("OR") || term.equals("NOT");
  }

  /**
   * Whether the field is one of the public person fields, which the public search can be sorted on.
   */
  static boolean isPublicField(@Nullable String field) {
    return field != null && PUBLIC_QUERY_FIELDS.stream()
      .anyMatch(f -> f.endsWith("\\*") ? field.startsWith(f.substring(0, f.length() - 2)) : field.equals(f));
  }

  /**
   * Find the persons that have a membership, in one of the configured roles, of the given studies, restricted to the
   * published ones the current subject can access. Listed from the database, sorted by last name.
   */
  public Documents<Person> findPublishedStudyMembers(int from, int limit, @Nullable String order,
                                                     Collection<String> studyIds) {
    return studyMembers(from, limit, order, studyIds, null);
  }

  private Documents<Person> studyMembers(int from, int limit, @Nullable String order, Collection<String> studyIds,
                                         @Nullable Collection<String> excludes) {
    List<String> ids = publishedStudyService.findAccessibleIds().stream()
      .filter(studyIds::contains)
      .collect(Collectors.toList());
    List<String> roles = micaConfigService.getConfig().getRoles();
    if (ids.isEmpty() || roles.isEmpty()) return new Documents<>(0, from, limit);
    return page(personRepository.findByMemberships(ids, List.of(), roles, byLastName(order)), excludes, from, limit, false);
  }

  /**
   * When the search engine fails, the persons are listed from the database, sorted by last name. The query is not
   * applied.
   */
  @Override
  protected Documents<Person> findFallback(int from, int limit, @Nullable String order, @Nullable String studyId,
                                           @Nullable Searcher.IdFilter idFilter) {
    // ponytail: loads all persons to page them, use a paged query if there are too many
    return page(personRepository.findAll(byLastName(order)), null, from, limit, true);
  }

  private static Sort byLastName(@Nullable String order) {
    return Sort.by("desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC, "lastName");
  }

  private static Documents<Person> page(List<Person> persons, @Nullable Collection<String> excludes, int from, int limit,
                                       boolean degraded) {
    List<Person> found = excludes == null ? persons
      : persons.stream().filter(p -> !excludes.contains(p.getId())).collect(Collectors.toList());
    Documents<Person> documents = new Documents<>(found.size(), from, limit);
    documents.setDegraded(degraded);
    found.stream().skip(from).limit(limit).forEach(documents::add);
    return documents;
  }

  public static String termsQuery(String field, Collection<String> values) {
    return field + ":(" + values.stream()
      .map(v -> "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
      .collect(Collectors.joining(" OR ")) + ")";
  }
}
