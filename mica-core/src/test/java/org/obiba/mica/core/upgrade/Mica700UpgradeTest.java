/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.runtime.Version;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = Mica700UpgradeTest.Config.class)
public class Mica700UpgradeTest {

  @Inject
  private Mica700Upgrade mica700Upgrade;

  @Inject
  private MongoTemplate mongoTemplate;

  @BeforeAll
  public static void init() {
    assumeTrue(isMongoAvailable(), "MongoDB is not available on localhost:27017");
  }

  private static boolean isMongoAvailable() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", 27017), 1000);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @BeforeEach
  public void clearDatabase() {
    mongoTemplate.getDb().drop();
  }

  private MongoCollection<Document> acls() {
    return mongoTemplate.getCollection("subjectAcl");
  }

  private void createEntity(String collection, String id) {
    mongoTemplate.getCollection(collection).insertOne(new Document("_id", id));
  }

  private void createAcl(String principal, String resource, String instance, String... actions) {
    Document acl = new Document("principal", principal).append("type", "USER").append("resource", resource)
      .append("actions", Arrays.asList(actions));
    if (instance != null) acl.append("instance", instance);
    acls().insertOne(acl);
  }

  private long countAcls(String resource, String instance) {
    Document query = new Document("resource", resource);
    if (instance != null) query.append("instance", instance);
    return acls().countDocuments(query);
  }

  private void execute() {
    mica700Upgrade.execute(new Version(6, 2, 0));
  }

  @Test
  public void test_legacy_external_editor_acl_gets_add_and_unused_child_acls_are_removed() {
    createEntity("study", "s1");
    // external editor: legacy VIEW,EDIT + _status/_attachments child acls
    createAcl("alice", "/draft/individual-study", "s1", "VIEW", "EDIT");
    createAcl("alice", "/draft/individual-study/s1", "_status", "EDIT");
    createAcl("alice", "/draft/individual-study/s1/_attachments", null, "EDIT");
    // regular user with VIEW,EDIT set some other way: left alone
    createAcl("bob", "/draft/individual-study", "s1", "VIEW", "EDIT");
    // comment acl is still in use
    createAcl("bob", "/draft/individual-study/s1/comment", "c1", "VIEW", "EDIT", "DELETE");

    execute();

    Document alice = acls().find(new Document("principal", "alice").append("resource", "/draft/individual-study"))
      .first();
    assertThat(alice.getList("actions", String.class)).containsExactly("VIEW", "EDIT", "ADD");
    Document bob = acls().find(new Document("principal", "bob").append("resource", "/draft/individual-study"))
      .first();
    assertThat(bob.getList("actions", String.class)).containsExactly("VIEW", "EDIT");
    assertThat(countAcls("/draft/individual-study/s1", "_status")).isZero();
    assertThat(countAcls("/draft/individual-study/s1/_attachments", null)).isZero();
    assertThat(countAcls("/draft/individual-study/s1/comment", "c1")).isEqualTo(1);
  }

  @Test
  public void test_orphan_acls_are_purged_and_existing_ones_kept() {
    createEntity("project", "p1");
    // p1 exists; p2 and p1-old (shares p1's prefix) were deleted
    for (String id : Arrays.asList("p1", "p2", "p1-old")) {
      createAcl("alice", "/project", id, "VIEW");
      createAcl("alice", "/draft/project", id, "VIEW", "EDIT", "ADD");
      createAcl("alice", "/draft/project/" + id + "/comment", "c-" + id, "VIEW", "EDIT", "DELETE");
      createAcl("alice", "/file", "/project/" + id, "VIEW");
      createAcl("alice", "/draft/file", "/project/" + id, "VIEW", "EDIT", "ADD");
      createAcl("alice", "/draft/file", "/project/" + id + "/doc.pdf", "VIEW");
    }
    // acls not tied to a single entity: left alone
    createAcl("alice", "/draft/project", "*", "VIEW");
    createAcl("alice", "/draft/file", "/project", "VIEW");
    createAcl("alice", "/data-access-request", "dar1", "VIEW");

    execute();

    for (String id : Arrays.asList("p2", "p1-old")) {
      assertThat(countAcls("/project", id)).isZero();
      assertThat(countAcls("/draft/project", id)).isZero();
      assertThat(countAcls("/draft/project/" + id + "/comment", null)).isZero();
      assertThat(countAcls("/file", "/project/" + id)).isZero();
      assertThat(countAcls("/draft/file", "/project/" + id)).isZero();
      assertThat(countAcls("/draft/file", "/project/" + id + "/doc.pdf")).isZero();
    }
    assertThat(countAcls("/project", "p1")).isEqualTo(1);
    assertThat(countAcls("/draft/project", "p1")).isEqualTo(1);
    assertThat(countAcls("/draft/project/p1/comment", "c-p1")).isEqualTo(1);
    assertThat(countAcls("/file", "/project/p1")).isEqualTo(1);
    assertThat(countAcls("/draft/file", "/project/p1")).isEqualTo(1);
    assertThat(countAcls("/draft/file", "/project/p1/doc.pdf")).isEqualTo(1);

    assertThat(countAcls("/draft/project", "*")).isEqualTo(1);
    assertThat(countAcls("/draft/file", "/project")).isEqualTo(1);
    assertThat(countAcls("/data-access-request", "dar1")).isEqualTo(1);
  }

  @Test
  public void test_encoded_instance_of_existing_entity_is_kept() {
    createEntity("network", "my net");
    // SubjectAclService stores instances url-encoded
    createAcl("alice", "/draft/network", "my+net", "VIEW", "EDIT", "ADD");
    createAcl("alice", "/draft/file", "/network/my+net", "VIEW");

    execute();

    assertThat(countAcls("/draft/network", "my+net")).isEqualTo(1);
    assertThat(countAcls("/draft/file", "/network/my+net")).isEqualTo(1);
  }

  @Configuration
  static class Config extends AbstractMongoClientConfiguration {

    @Bean
    public Mica700Upgrade mica700Upgrade(MongoTemplate mongoTemplate) {
      return new Mica700Upgrade(mongoTemplate);
    }

    @Override
    protected String getDatabaseName() {
      return "mica-test";
    }
  }
}
