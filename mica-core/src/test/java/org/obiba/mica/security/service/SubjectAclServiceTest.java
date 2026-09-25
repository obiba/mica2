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

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.config.JsonConfiguration;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetDeletedEvent;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.NetworkDeletedEvent;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.ProjectDeletedEvent;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.StudyDeletedEvent;
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
   * Populates the whole set of ACLs an entity can accumulate: published + draft entity ACL, draft child
   * ACL (comment), and file + draft-file ACLs for the entity and a sub-path.
   */
  private void createAcls(String resource, String id) {
    String draftResource = "/draft" + resource;
    subjectAclService.addUserPermission("editor", resource, "VIEW", id);
    subjectAclService.addUserPermission("editor", draftResource, "VIEW,EDIT,ADD", id);
    subjectAclService.addUserPermission("editor", draftResource + "/" + id + "/comment", "VIEW,EDIT", "c1");
    subjectAclService.addUserPermission("editor", "/file", "VIEW", resource + "/" + id);
    subjectAclService.addUserPermission("editor", "/file", "VIEW", resource + "/" + id + "/sub-file");
    subjectAclService.addUserPermission("editor", "/draft/file", "VIEW", resource + "/" + id);
    subjectAclService.addUserPermission("editor", "/draft/file", "VIEW", resource + "/" + id + "/sub-file");
  }

  private void assertNoAclsLeftFor(String resource, String id) {
    String draftResource = "/draft" + resource;
    assertThat(subjectAclRepository.findByResourceAndInstance(resource, id)).isEmpty();
    assertThat(subjectAclRepository.findByResourceAndInstance(draftResource, id)).isEmpty();
    assertThat(subjectAclRepository.findByResource(draftResource + "/" + id)).isEmpty();
    assertThat(subjectAclRepository.findByResourceStartingWith(draftResource + "/" + id + "/")).isEmpty();
    assertThat(subjectAclRepository.findByResourceAndInstance("/file", resource + "/" + id)).isEmpty();
    assertThat(subjectAclRepository.findByResourceAndInstance("/file", resource + "/" + id + "/sub-file")).isEmpty();
    assertThat(subjectAclRepository.findByResourceAndInstance("/draft/file", resource + "/" + id)).isEmpty();
    assertThat(subjectAclRepository.findByResourceAndInstance("/draft/file", resource + "/" + id + "/sub-file")).isEmpty();
  }

  private int countAclsFor(String resource, String id) {
    String draftResource = "/draft" + resource;
    int count = subjectAclRepository.findByResourceAndInstance(resource, id).size();
    count += subjectAclRepository.findByResourceAndInstance(draftResource, id).size();
    count += subjectAclRepository.findByResource(draftResource + "/" + id).size();
    count += subjectAclRepository.findByResourceStartingWith(draftResource + "/" + id + "/").size();
    count += subjectAclRepository.findByResourceAndInstance("/file", resource + "/" + id).size();
    count += subjectAclRepository.findByResourceAndInstance("/file", resource + "/" + id + "/sub-file").size();
    count += subjectAclRepository.findByResourceAndInstance("/draft/file", resource + "/" + id).size();
    count += subjectAclRepository.findByResourceAndInstance("/draft/file", resource + "/" + id + "/sub-file").size();
    return count;
  }

  @Test
  public void test_projectDeleted_removes_all_acls() {
    createAcls("/project", "abc");
    Project project = new Project();
    project.setId("abc");

    subjectAclService.projectDeleted(new ProjectDeletedEvent(project));

    assertNoAclsLeftFor("/project", "abc");
  }

  @Test
  public void test_networkDeleted_removes_all_acls_including_draft_children() {
    createAcls("/network", "abc");
    Network network = new Network();
    network.setId("abc");

    subjectAclService.networkDeleted(new NetworkDeletedEvent(network));

    assertNoAclsLeftFor("/network", "abc");
  }

  @Test
  public void test_studyDeleted_removes_all_acls_for_both_study_kinds() {
    createAcls("/individual-study", "abc");
    Study study = new Study();
    study.setId("abc");
    subjectAclService.studyDeleted(new StudyDeletedEvent(study));
    assertNoAclsLeftFor("/individual-study", "abc");

    createAcls("/harmonization-study", "abc");
    HarmonizationStudy harmonizationStudy = new HarmonizationStudy();
    harmonizationStudy.setId("abc");
    subjectAclService.studyDeleted(new StudyDeletedEvent(harmonizationStudy));
    assertNoAclsLeftFor("/harmonization-study", "abc");
  }

  @Test
  public void test_datasetDeleted_removes_all_acls_for_both_dataset_kinds() {
    createAcls("/collected-dataset", "abc");
    StudyDataset studyDataset = new StudyDataset();
    studyDataset.setId("abc");
    subjectAclService.datasetDeleted(new DatasetDeletedEvent(studyDataset));
    assertNoAclsLeftFor("/collected-dataset", "abc");

    createAcls("/harmonized-dataset", "abc");
    HarmonizationDataset harmonizationDataset = new HarmonizationDataset();
    harmonizationDataset.setId("abc");
    subjectAclService.datasetDeleted(new DatasetDeletedEvent(harmonizationDataset));
    assertNoAclsLeftFor("/harmonized-dataset", "abc");
  }

  @Test
  public void test_deleting_entity_leaves_sibling_id_with_shared_prefix_intact() {
    createAcls("/project", "abc");
    createAcls("/project", "abcd");

    Project project = new Project();
    project.setId("abc");
    subjectAclService.projectDeleted(new ProjectDeletedEvent(project));

    assertNoAclsLeftFor("/project", "abc");
    assertThat(countAclsFor("/project", "abcd")).isEqualTo(7);
  }

  @Test
  public void test_legacy_view_edit_acl_is_fully_removed() {
    subjectAclService.addUserPermission("external-editor", "/draft/individual-study", "VIEW,EDIT", "abc");

    subjectAclService.removeUserPermission("external-editor", "/draft/individual-study", "VIEW,EDIT,ADD", "abc");

    assertThat(subjectAclRepository.findByResourceAndInstance("/draft/individual-study", "abc")).isEmpty();
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
