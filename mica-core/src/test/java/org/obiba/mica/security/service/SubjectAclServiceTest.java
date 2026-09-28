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

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessRequestDeletedEvent;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

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
   * Every kind of ACL a data access request can accumulate, including children the delete handler does not name
   * (feasibility, agreement, "comments").
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

  private int countDataAccessRequestAcls(String id) {
    return subjectAclRepository.findByResourceAndInstance("/data-access-request", id).size()
      + subjectAclRepository.findByResourceStartingWith("/data-access-request/" + id + "/").size()
      + subjectAclRepository.findByResource("/data-access-request/" + id).size();
  }

  @Test
  public void test_dataAccessRequestDeleted_removes_all_child_acls_and_leaves_sibling_intact() {
    createDataAccessRequestAcls("dar1");
    createDataAccessRequestAcls("dar10");
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().build();
    request.setId("dar1");

    subjectAclService.dataAccessRequestDeleted(new DataAccessRequestDeletedEvent(request));

    assertThat(countDataAccessRequestAcls("dar1")).isZero();
    assertThat(countDataAccessRequestAcls("dar10")).isEqualTo(10);
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
