/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import com.google.common.base.Strings;
import org.obiba.mica.core.domain.Person;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class PersonRepositoryImpl implements PersonRepositoryCustom {

  private static final String VERSION = "version";

  private static final String LAST_MODIFIED_DATE = "lastModifiedDate";

  private static final String STUDY_MEMBERSHIPS = "studyMemberships";

  private static final String NETWORK_MEMBERSHIPS = "networkMemberships";

  private final MongoTemplate mongoTemplate;

  @Inject
  public PersonRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public boolean updateDetails(String id, long version, Person details) {
    Update update = new Update().set(LAST_MODIFIED_DATE, LocalDateTime.now()).inc(VERSION, 1);
    // an absent field is cleared, as the whole document saves did
    setOrUnset(update, "title", details.getTitle());
    setOrUnset(update, "firstName", details.getFirstName());
    setOrUnset(update, "lastName", details.getLastName());
    setOrUnset(update, "academicLevel", details.getAcademicLevel());
    setOrUnset(update, "email", details.getEmail());
    setOrUnset(update, "phone", details.getPhone());
    setOrUnset(update, "institution", details.getInstitution());
    return mongoTemplate.updateFirst(withVersion(id, version), update, Person.class).getMatchedCount() > 0;
  }

  @Override
  public boolean addStudyMembership(String id, String studyId, String role) {
    return addMembership(id, STUDY_MEMBERSHIPS, studyId, role);
  }

  @Override
  public boolean removeStudyMembership(String id, String studyId, String role) {
    return removeMembership(id, STUDY_MEMBERSHIPS, studyId, role);
  }

  @Override
  public boolean addNetworkMembership(String id, String networkId, String role) {
    return addMembership(id, NETWORK_MEMBERSHIPS, networkId, role);
  }

  @Override
  public boolean removeNetworkMembership(String id, String networkId, String role) {
    return removeMembership(id, NETWORK_MEMBERSHIPS, networkId, role);
  }

  @Override
  public List<String> removeAllStudyMemberships(String studyId) {
    return pullAll(where(STUDY_MEMBERSHIPS + ".parentId").is(studyId),
      new Update().pull(STUDY_MEMBERSHIPS, Query.query(where("parentId").is(studyId))));
  }

  @Override
  public List<String> removeAllNetworkMemberships(String networkId) {
    return pullAll(where(NETWORK_MEMBERSHIPS + ".parentId").is(networkId),
      new Update().pull(NETWORK_MEMBERSHIPS, Query.query(where("parentId").is(networkId))));
  }

  @Override
  public List<String> removeAllMembershipsWithRole(String role) {
    return pullAll(new Criteria().orOperator(where(STUDY_MEMBERSHIPS + ".role").is(role),
        where(NETWORK_MEMBERSHIPS + ".role").is(role)),
      new Update()
        .pull(STUDY_MEMBERSHIPS, Query.query(where("role").is(role)))
        .pull(NETWORK_MEMBERSHIPS, Query.query(where("role").is(role))));
  }

  @Override
  public boolean replace(Person person, long version) {
    person.setVersion(version + 1);
    person.setLastModifiedDate(LocalDateTime.now());
    return mongoTemplate.findAndReplace(withVersion(person.getId(), version), person) != null;
  }

  @Override
  public boolean deleteIfUnchanged(Person read) {
    Query query = withVersion(read.getId(), read.getVersionValue());
    query.addCriteria(where(LAST_MODIFIED_DATE).is(read.getLastModifiedDate().orElse(null)));
    return mongoTemplate.remove(query, Person.class).getDeletedCount() > 0;
  }

  @Override
  public List<Person> findByEmailIgnoreCase(String email) {
    String trimmed = Strings.nullToEmpty(email).trim();
    if (trimmed.isEmpty()) return List.of();
    // exact match, the email is quoted: regex characters (e.g. "+") match only themselves; the stored email may have
    // surrounding spaces (stored as sent), ignored like in PersonService.sameEmail
    return mongoTemplate.find(
      Query.query(where("email").regex("^\\s*" + Pattern.quote(trimmed) + "\\s*$", "i")), Person.class);
  }

  private boolean addMembership(String id, String field, String parentId, String role) {
    return mongoTemplate.updateFirst(Query.query(where("_id").is(id)),
      new Update().addToSet(field, new Person.Membership(parentId, role)).set(LAST_MODIFIED_DATE, LocalDateTime.now()),
      Person.class).getMatchedCount() > 0;
  }

  private boolean removeMembership(String id, String field, String parentId, String role) {
    return mongoTemplate.updateFirst(Query.query(where("_id").is(id)),
      new Update().pull(field, Query.query(where("parentId").is(parentId).and("role").is(role)))
        .set(LAST_MODIFIED_DATE, LocalDateTime.now()),
      Person.class).getMatchedCount() > 0;
  }

  /**
   * Pulled person by person, for the history and the index of each: repeated until no person matches, so a person
   * linked meanwhile is pulled and listed too.
   */
  private List<String> pullAll(Criteria criteria, Update update) {
    List<String> pulled = new ArrayList<>();
    boolean modified;
    do {
      modified = false;
      Query query = Query.query(criteria);
      query.fields().include("_id");
      for (Person person : mongoTemplate.find(query, Person.class)) {
        String id = person.getId();
        Query one = Query.query(new Criteria().andOperator(where("_id").is(id), criteria));
        if (mongoTemplate.updateFirst(one, update.set(LAST_MODIFIED_DATE, LocalDateTime.now()), Person.class)
          .getModifiedCount() > 0) {
          modified = true;
          if (!pulled.contains(id)) pulled.add(id);
        }
      }
    } while (modified); // stops also if a matching person could not be pulled, no endless loop
    return pulled;
  }

  /** the person with this id and version, 0 matching a document without version */
  private static Query withVersion(String id, long version) {
    Criteria criteria = where("_id").is(id);
    return Query.query(version == 0
      ? criteria.and(VERSION).in(Arrays.asList(0L, null))
      : criteria.and(VERSION).is(version));
  }

  private static void setOrUnset(Update update, String field, Object value) {
    if (value == null) update.unset(field);
    else update.set(field, value);
  }
}
