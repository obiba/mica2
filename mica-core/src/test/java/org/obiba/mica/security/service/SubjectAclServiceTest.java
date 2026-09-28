/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessAmendmentDeletedEvent;
import org.obiba.mica.access.event.DataAccessFeasibilityDeletedEvent;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.ResourceDeletedEvent;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.obiba.mica.assertj.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = { SubjectAclServiceTest.Config.class, JsonConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SubjectAclServiceTest {

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private SubjectAclRepository subjectAclRepository;

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

  /**
   * Every kind of ACL a data access request can accumulate: the request, its status and all its children
   * (attachments, amendment, feasibility, agreement, comments).
   */
  private void createDataAccessRequestAcls(String id) {
    String resource = "/data-access-request";
    String dar = resource + "/" + id;
    subjectAclService.addUserPermission("applicant", resource, "VIEW,EDIT,DELETE", id);
    subjectAclService.addUserPermission("applicant", dar, "EDIT", "_status");
    subjectAclService.addUserPermission("applicant", dar + "/_attachments", "EDIT", null);
    subjectAclService.addUserPermission("applicant", dar + "/amendment", "VIEW,EDIT,DELETE", "a1");
    subjectAclService.addUserPermission("applicant", dar + "/amendment/a1", "EDIT", "_status");
    subjectAclService.addUserPermission("applicant", dar + "/feasibility", "VIEW,EDIT,DELETE", "f1");
    subjectAclService.addUserPermission("applicant", dar + "/feasibility/f1", "EDIT", "_status");
    subjectAclService.addUserPermission("applicant", dar + "/agreement", "VIEW,EDIT,DELETE", "g1");
    subjectAclService.addUserPermission("applicant", dar + "/comment", "VIEW,EDIT,DELETE", "c1");
    subjectAclService.addUserPermission("applicant", dar + "/comments", "VIEW,EDIT,DELETE", "c2");
  }

  /**
   * ACLs shared by all data access requests: role-level (no instance or "*") and global resources below
   * "/data-access-request/". No request deletion may remove them.
   */
  private void createGlobalDataAccessAcls() {
    subjectAclService.addGroupPermission("mica-data-access-officer", "/data-access-request", "VIEW,EDIT,DELETE", null);
    subjectAclService.addGroupPermission("mica-reviewer", "/data-access-request", "VIEW", "*");
    subjectAclService.addGroupPermission("mica-data-access-officer", "/data-access-request/private-comment", "VIEW", null);
  }

  private List<SubjectAcl> findByResource(String resource) {
    return mongoTemplate.find(Query.query(Criteria.where("resource").is(resource)), SubjectAcl.class);
  }

  private int countDataAccessRequestAcls(String id) {
    return subjectAclRepository.findByResourceAndInstance("/data-access-request", id).size()
      + subjectAclRepository.findByResourceStartingWith("/data-access-request/" + id + "/").size()
      + findByResource("/data-access-request/" + id).size();
  }

  @Test
  public void test_dataAccessRequestDeleted_removes_all_child_acls_and_leaves_sibling_intact() {
    createGlobalDataAccessAcls();
    createDataAccessRequestAcls("dar1");
    createDataAccessRequestAcls("dar10");
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().build();
    request.setId("dar1");

    subjectAclService.dataAccessRequestDeleted(new DataAccessRequestDeletedEvent(request));

    assertThat(countDataAccessRequestAcls("dar1")).isZero();
    assertThat(countDataAccessRequestAcls("dar10")).isEqualTo(10);
    // only dar10 and the global acls are left
    assertThat(subjectAclRepository.count()).isEqualTo(10 + 3);
  }

  /**
   * A feasibility or an amendment deleted on its own (the request is kept) removes only its own acls.
   */
  @Test
  public void test_child_deleted_removes_only_its_own_acls() {
    createGlobalDataAccessAcls();
    createDataAccessRequestAcls("dar1");
    createDataAccessRequestAcls("dar10");
    // siblings of f1 and a1 in the same request, whose ids start with the deleted ones
    subjectAclService.addUserPermission("applicant", "/data-access-request/dar1/feasibility", "VIEW,EDIT,DELETE", "f10");
    subjectAclService.addUserPermission("applicant", "/data-access-request/dar1/feasibility/f10", "EDIT", "_status");
    subjectAclService.addUserPermission("applicant", "/data-access-request/dar1/amendment", "VIEW,EDIT,DELETE", "a10");
    subjectAclService.addUserPermission("applicant", "/data-access-request/dar1/amendment/a10", "EDIT", "_status");
    long total = subjectAclRepository.count();

    DataAccessFeasibility feasibility = (DataAccessFeasibility) DataAccessFeasibility.newBuilder().build();
    feasibility.setId("f1");
    feasibility.setParentId("dar1");
    subjectAclService.dataAccessFeasibilityDeleted(new DataAccessFeasibilityDeletedEvent(feasibility));

    assertThat(subjectAclRepository.findByResourceAndInstance("/data-access-request/dar1/feasibility", "f1")).isEmpty();
    assertThat(findByResource("/data-access-request/dar1/feasibility/f1")).isEmpty();
    assertThat(subjectAclRepository.count()).isEqualTo(total - 2);

    DataAccessAmendment amendment = (DataAccessAmendment) DataAccessAmendment.newBuilder().build();
    amendment.setId("a1");
    amendment.setParentId("dar1");
    subjectAclService.dataAccessAmendmentDeleted(new DataAccessAmendmentDeletedEvent(amendment));

    assertThat(subjectAclRepository.findByResourceAndInstance("/data-access-request/dar1/amendment", "a1")).isEmpty();
    assertThat(findByResource("/data-access-request/dar1/amendment/a1")).isEmpty();
    assertThat(subjectAclRepository.count()).isEqualTo(total - 4);

    // the request itself, the sibling children and the other request are untouched
    assertThat(countDataAccessRequestAcls("dar1")).isEqualTo(10 + 4 - 4);
    assertThat(subjectAclRepository.findByResourceAndInstance("/data-access-request/dar1/feasibility", "f10")).hasSize(1);
    assertThat(findByResource("/data-access-request/dar1/feasibility/f10")).hasSize(1);
    assertThat(subjectAclRepository.findByResourceAndInstance("/data-access-request/dar1/amendment", "a10")).hasSize(1);
    assertThat(findByResource("/data-access-request/dar1/amendment/a10")).hasSize(1);
    assertThat(countDataAccessRequestAcls("dar10")).isEqualTo(10);
  }

  /**
   * Deleting a comment (ResourceDeletedEvent with the comment id) removes only the acls of that comment.
   */
  @Test
  public void test_commentDeleted_removes_only_its_own_acls() {
    createGlobalDataAccessAcls();
    createDataAccessRequestAcls("dar1");
    createDataAccessRequestAcls("dar10");
    // as granted by DataAccessRequestResource.createComment: author and data access officers
    subjectAclService.addGroupPermission("mica-data-access-officer", "/data-access-request/dar1/comment", "DELETE", "c1");
    subjectAclService.addUserPermission("applicant", "/data-access-request/dar1/comment", "VIEW,EDIT,DELETE", "c10");
    long total = subjectAclRepository.count();

    subjectAclService.onResourceDeleted(new ResourceDeletedEvent("/data-access-request/dar1/comment", "c1"));

    assertThat(subjectAclRepository.findByResourceAndInstance("/data-access-request/dar1/comment", "c1")).isEmpty();
    assertThat(subjectAclRepository.findByResourceAndInstance("/data-access-request/dar1/comment", "c10")).hasSize(1);
    assertThat(subjectAclRepository.count()).isEqualTo(total - 2);
    assertThat(countDataAccessRequestAcls("dar10")).isEqualTo(10);
  }

  /**
   * Deleting a data access request also deletes its feasibilities and amendments, whose async listeners remove
   * overlapping acls at the same time. None of them may fail on an acl another one already removed.
   */
  @Test
  public void test_dataAccessRequestDeleted_concurrently_with_children_deleted_removes_all_acls() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(3);
    try {
      for (int i = 0; i < 20; i++) {
        createDataAccessRequestAcls("dar1");
        createDataAccessRequestAcls("dar10");

        DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().build();
        request.setId("dar1");
        DataAccessFeasibility feasibility = (DataAccessFeasibility) DataAccessFeasibility.newBuilder().build();
        feasibility.setId("f1");
        feasibility.setParentId("dar1");
        DataAccessAmendment amendment = (DataAccessAmendment) DataAccessAmendment.newBuilder().build();
        amendment.setId("a1");
        amendment.setParentId("dar1");

        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> deletions = Lists.newArrayList(
          executor.submit(() -> awaitThen(start, () -> subjectAclService.dataAccessRequestDeleted(new DataAccessRequestDeletedEvent(request)))),
          executor.submit(() -> awaitThen(start, () -> subjectAclService.dataAccessFeasibilityDeleted(new DataAccessFeasibilityDeletedEvent(feasibility)))),
          executor.submit(() -> awaitThen(start, () -> subjectAclService.dataAccessAmendmentDeleted(new DataAccessAmendmentDeletedEvent(amendment)))));
        start.countDown();
        for (Future<?> deletion : deletions) deletion.get(10, TimeUnit.SECONDS); // rethrows any listener failure

        assertThat(countDataAccessRequestAcls("dar1")).isZero();
        assertThat(countDataAccessRequestAcls("dar10")).isEqualTo(10);
        mongoTemplate.getDb().drop();
      }
    } finally {
      executor.shutdownNow();
    }
  }

  /**
   * Without an instance the children prefix would be the bare resource and match the acls of every instance.
   */
  @Test
  public void test_resourceDeleted_without_instance_is_rejected_and_removes_nothing() {
    createDataAccessRequestAcls("dar1");
    createDataAccessRequestAcls("dar10");
    long total = subjectAclRepository.count();

    assertThatThrownBy(() -> subjectAclService.onResourceDeleted(new ResourceDeletedEvent("/data-access-request", null)))
      .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> subjectAclService.onResourceDeleted(new ResourceDeletedEvent("/data-access-request", "")))
      .isInstanceOf(IllegalArgumentException.class);

    assertThat(subjectAclRepository.count()).isEqualTo(total);
    assertThat(countDataAccessRequestAcls("dar1")).isEqualTo(10);
    assertThat(countDataAccessRequestAcls("dar10")).isEqualTo(10);
  }

  private static void awaitThen(CountDownLatch start, Runnable deletion) {
    try {
      start.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    deletion.run();
  }

  @Configuration
  @EnableMongoRepositories("org.obiba.mica.security.repository")
  static class Config extends AbstractMongoClientConfiguration {

    @Bean
    public SubjectAclService subjectAclService() {
      return new SubjectAclService();
    }

    @Bean
    public MicaConfigService micaConfigService() {
      return mock(MicaConfigService.class);
    }

    @Bean
    public org.obiba.mica.micaConfig.repository.MicaConfigRepository micaConfigRepository() {
      return mock(org.obiba.mica.micaConfig.repository.MicaConfigRepository.class);
    }

    @Bean
    public org.obiba.mica.micaConfig.service.TaxonomyConfigService taxonomyConfigService() {
      return mock(org.obiba.mica.micaConfig.service.TaxonomyConfigService.class);
    }

    @Bean
    public org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository taxonomyConfigRepository() {
      return mock(org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository.class);
    }

    @Bean
    public EventBus eventBus() {
      return mock(EventBus.class);
    }

    @Override
    protected String getDatabaseName() {
      return "mica-test";
    }

    @Override
    @Bean
    public MongoCustomConversions customConversions() {
      return new MongoCustomConversions(
        Lists.newArrayList(new MongoDbConfiguration.LocalizedStringWriteConverter(),
          new MongoDbConfiguration.LocalizedStringReadConverter()));
    }
  }
}
