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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.core.domain.Person;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The person writes change only what they are about, against a MongoDB on localhost:27017 (#4753).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PersonRepositoryImplTest.Config.class)
public class PersonRepositoryImplTest {

  @Inject
  private PersonRepository personRepository;

  @Inject
  private MongoTemplate mongoTemplate;

  @BeforeAll
  public static void init() {
    assumeTrue(isMongoAvailable(), "MongoDB is not available on localhost:27017");
  }

  @BeforeEach
  public void clearDatabase() {
    mongoTemplate.dropCollection(Person.class);
  }

  @Test
  public void document_without_version_is_updated_with_version_0_and_gets_1() {
    insertRaw(new Document("_id", "p1").append("lastName", "Doe"));

    assertThat(personRepository.updateDetails("p1", 1L, details("Smith"))).isFalse();
    assertThat(personRepository.updateDetails("p1", 0L, details("Smith"))).isTrue();

    Person stored = personRepository.findById("p1").orElseThrow();
    assertThat(stored.getLastName()).isEqualTo("Smith");
    assertThat(stored.getVersion()).isEqualTo(1L);
  }

  @Test
  public void stale_version_does_not_update() {
    Person person = insert("p1", 2L);

    assertThat(personRepository.updateDetails("p1", 1L, details("Smith"))).isFalse();
    assertThat(personRepository.findById("p1").orElseThrow().getLastName()).isEqualTo(person.getLastName());
  }

  @Test
  public void details_update_keeps_memberships_and_data_access_committee_and_clears_absent_fields() {
    Person person = person("p1", 0L);
    person.setPhone("123");
    person.setDataAccessCommitteeMember(true);
    person.getStudyMemberships().add(new Person.Membership("s1", "contact"));
    mongoTemplate.insert(person);
    // a link added after the person was loaded for the update
    personRepository.addStudyMembership("p1", "s2", "investigator");

    assertThat(personRepository.updateDetails("p1", 0L, details("Smith"))).isTrue();

    Person stored = personRepository.findById("p1").orElseThrow();
    assertThat(stored.getStudyMemberships()).containsExactly(
      new Person.Membership("s1", "contact"), new Person.Membership("s2", "investigator"));
    assertThat(stored.isDataAccessCommitteeMember()).isTrue();
    assertThat(stored.getPhone()).isNull();
  }

  @Test
  public void link_and_unlink_change_one_membership_and_not_the_version() {
    insert("p1", 4L);

    assertThat(personRepository.addNetworkMembership("p1", "n1", "contact")).isTrue();
    assertThat(personRepository.addNetworkMembership("p1", "n1", "contact")).isTrue();
    assertThat(personRepository.addNetworkMembership("p1", "n1", "investigator")).isTrue();
    assertThat(personRepository.removeNetworkMembership("p1", "n1", "contact")).isTrue();
    assertThat(personRepository.addStudyMembership("missing", "s1", "contact")).isFalse();

    Person stored = personRepository.findById("p1").orElseThrow();
    assertThat(stored.getNetworkMemberships()).containsExactly(new Person.Membership("n1", "investigator"));
    assertThat(stored.getVersion()).isEqualTo(4L);
  }

  @Test
  public void study_deletion_and_role_removal_pull_the_memberships() {
    Person p1 = person("p1", 0L);
    p1.getStudyMemberships().addAll(Lists.newArrayList(
      new Person.Membership("s1", "contact"), new Person.Membership("s2", "contact")));
    mongoTemplate.insert(p1);
    Person p2 = person("p2", 0L);
    p2.getNetworkMemberships().add(new Person.Membership("n1", "investigator"));
    mongoTemplate.insert(p2);

    assertThat(personRepository.removeAllStudyMemberships("s1")).containsExactly("p1");
    assertThat(personRepository.removeAllMembershipsWithRole("investigator")).containsExactly("p2");

    assertThat(personRepository.findById("p1").orElseThrow().getStudyMemberships())
      .containsExactly(new Person.Membership("s2", "contact"));
    assertThat(personRepository.findById("p2").orElseThrow().getNetworkMemberships()).isEmpty();
  }

  @Test
  public void replace_is_guarded_by_the_version() {
    insertRaw(new Document("_id", "p1").append("lastName", "Doe"));
    Person restored = person("p1", null);
    restored.setLastName("Restored");

    assertThat(personRepository.replace(restored, 3L)).isFalse();
    assertThat(personRepository.replace(restored, 0L)).isTrue();

    Person stored = personRepository.findById("p1").orElseThrow();
    assertThat(stored.getLastName()).isEqualTo("Restored");
    assertThat(stored.getVersion()).isEqualTo(1L);
  }

  @Test
  public void delete_if_unchanged_keeps_a_person_written_since_read() {
    insert("p1", 0L);
    insert("p2", 0L);
    Person readP1 = personRepository.findById("p1").orElseThrow();
    Person readP2 = personRepository.findById("p2").orElseThrow();
    personRepository.addStudyMembership("p2", "s1", "contact"); // linked after the read

    assertThat(personRepository.deleteIfUnchanged(readP1)).isTrue();
    assertThat(personRepository.deleteIfUnchanged(readP2)).isFalse();
    assertThat(personRepository.findById("p1")).isEmpty();
    assertThat(personRepository.findById("p2").orElseThrow().getStudyMemberships())
      .containsExactly(new Person.Membership("s1", "contact"));
  }

  @Test
  public void email_lookup_is_exact_trimmed_and_ignores_case() {
    Person john = person("p1", 0L);
    john.setEmail("john@x.org");
    mongoTemplate.insert(john);
    Person aab = person("p2", 0L);
    aab.setEmail("aab@x.org");
    mongoTemplate.insert(aab);

    assertThat(personRepository.findByEmailIgnoreCase(" John@X.org ")).extracting(Person::getId).containsExactly("p1");
    assertThat(personRepository.findByEmailIgnoreCase("john@x.or")).isEmpty();
    assertThat(personRepository.findByEmailIgnoreCase("a+b@x.org")).isEmpty();
    assertThat(personRepository.findByEmailIgnoreCase("")).isEmpty();
  }

  private Person insert(String id, Long version) {
    return mongoTemplate.insert(person(id, version));
  }

  private void insertRaw(Document document) {
    mongoTemplate.getCollection(mongoTemplate.getCollectionName(Person.class)).insertOne(document);
  }

  private static Person person(String id, Long version) {
    Person person = new Person();
    person.setId(id);
    person.setVersion(version);
    person.setLastName("Doe");
    return person;
  }

  private static Person details(String lastName) {
    Person details = new Person();
    details.setLastName(lastName);
    return details;
  }

  private static boolean isMongoAvailable() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", 27017), 1000);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @EnableMongoRepositories(basePackageClasses = PersonRepository.class)
  static class Config extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
      return "mica-test-persons";
    }

    @Override
    public MongoCustomConversions customConversions() {
      return new MongoCustomConversions(
        Lists.newArrayList(new MongoDbConfiguration.LocalizedStringWriteConverter(),
          new MongoDbConfiguration.LocalizedStringReadConverter()));
    }
  }
}
