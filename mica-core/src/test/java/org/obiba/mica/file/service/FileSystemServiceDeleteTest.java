/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.file.event.FolderDeletedEvent;
import org.obiba.mica.file.notification.FilePublicationFlowMailNotification;
import org.obiba.mica.study.domain.Study;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = FileSystemServiceDeleteTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FileSystemServiceDeleteTest {

  private FileSystemService fileSystemService;

  private EventBus eventBus;

  private FileStoreService fileStoreService;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private AttachmentStateRepository attachmentStateRepository;

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
  public void setUp() {
    mongoTemplate.getDb().drop();
    // not a bean: Spring would try to inject the fields of the mocked services
    fileSystemService = new FileSystemService();
    ReflectionTestUtils.setField(fileSystemService, "attachmentRepository", attachmentRepository);
    ReflectionTestUtils.setField(fileSystemService, "attachmentStateRepository", attachmentStateRepository);
    eventBus = mock(EventBus.class);
    ReflectionTestUtils.setField(fileSystemService, "eventBus", eventBus);
    fileStoreService = mock(FileStoreService.class);
    ReflectionTestUtils.setField(fileSystemService, "fileStoreService", fileStoreService);
  }

  /**
   * The path is matched literally: '.' and '(' in an entity id or folder name are not regex characters, so the files
   * of another path that would match them as a regex are kept.
   */
  @Test
  public void test_delete_path_removes_folder_and_descendants_only() {
    String folder = "/individual-study/cohort.v1 (a)";
    createFile(folder, FileSystemService.DIR_NAME);
    createFile(folder, "doc.pdf");
    createFile(folder + "/population", FileSystemService.DIR_NAME);
    createFile(folder + "/population", "sop.pdf");
    // would match "^/individual-study/cohort.v1 (a)" as a regex, or as a prefix without a path boundary
    createFile("/individual-study/cohortXv1 a", "doc.pdf");
    createFile("/individual-study/cohort.v1 (a)2", "doc.pdf");

    fileSystemService.delete(folder);

    assertThat(attachmentStateRepository.findAll()).extracting(AttachmentState::getPath)
      .containsExactlyInAnyOrder("/individual-study/cohortXv1 a", "/individual-study/cohort.v1 (a)2");
    assertThat(attachmentRepository.findAll()).extracting(Attachment::getPath)
      .containsExactlyInAnyOrder("/individual-study/cohortXv1 a", "/individual-study/cohort.v1 (a)2");
  }

  /**
   * An entity's delete deletes the folder its files are stored in, e.g. "/individual-study/<id>" for a study.
   */
  @Test
  public void test_delete_entity_path_removes_the_entity_files() {
    createFile("/individual-study/s1", FileSystemService.DIR_NAME);
    createFile("/individual-study/s1", "doc.pdf");
    Study study = new Study();
    study.setId("s1");

    fileSystemService.delete(FileUtils.getEntityPath(study));

    assertThat(attachmentStateRepository.count()).isZero();
    assertThat(attachmentRepository.count()).isZero();
  }

  /**
   * The files of a folder are flagged as deleted with it, and a single folder event follows them.
   */
  @Test
  public void test_delete_path_posts_one_folder_event_after_its_files() {
    createFile("/network/n1", FileSystemService.DIR_NAME);
    createFile("/network/n1", "doc.pdf");

    fileSystemService.delete("/network/n1");

    ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
    verify(eventBus, atLeastOnce()).post(events.capture());
    List<Object> posted = events.getAllValues();
    assertThat(posted).filteredOn(FileDeletedEvent.class::isInstance).hasSize(2)
      .allMatch(e -> ((FileDeletedEvent) e).isInFolderDelete());
    assertThat(posted).filteredOn(FolderDeletedEvent.class::isInstance).hasSize(1);
    Object last = posted.get(posted.size() - 1);
    assertThat(last).isInstanceOf(FolderDeletedEvent.class);
    assertThat(((FolderDeletedEvent) last).getPath()).isEqualTo("/network/n1");
  }

  /**
   * A failed file delete still posts the folder event, for the acls of the files deleted before it.
   */
  @Test
  public void test_delete_path_posts_folder_event_when_a_file_delete_fails() {
    createFile("/network/n1", "doc.pdf");
    doThrow(new RuntimeException("store failure")).when(fileStoreService).deleteWithMetadata(anyString(), any());

    assertThatThrownBy(() -> fileSystemService.delete("/network/n1")).hasMessage("store failure");

    verify(eventBus).post(any(FolderDeletedEvent.class));
  }

  /**
   * Folder operations other than delete match the path literally too.
   */
  @Test
  public void test_folder_operations_match_the_path_literally() {
    ReflectionTestUtils.setField(fileSystemService, "filePublicationFlowNotification",
      mock(FilePublicationFlowMailNotification.class));
    String folder = "/network/n.1 (a)";
    createFile(folder, FileSystemService.DIR_NAME);
    createFile(folder, "doc.pdf");
    createFile(folder + "/sub", FileSystemService.DIR_NAME);
    createFile("/network/nX1 (a)", FileSystemService.DIR_NAME);
    createFile("/network/nX1 (a)", "doc.pdf");

    assertThat(fileSystemService.countAttachmentStates(folder, false)).isEqualTo(2);
    assertThat(fileSystemService.hasAttachmentState(folder, "doc.pdf", false)).isTrue();

    fileSystemService.updateStatus(folder, RevisionStatus.DELETED);

    assertThat(attachmentStateRepository.findAll())
      .allMatch(s -> s.getPath().startsWith(folder) == (s.getRevisionStatus() == RevisionStatus.DELETED));
  }

  @Test
  public void test_delete_path_without_files_posts_nothing() {
    fileSystemService.delete("/network/n1");

    verifyNoInteractions(eventBus);
  }

  private void createFile(String path, String name) {
    Attachment attachment = new Attachment();
    attachment.setId(new ObjectId().toString());
    attachment.setPath(path);
    attachment.setName(name);
    attachment.setFileReference(attachment.getId());
    attachmentRepository.insert(attachment);
    AttachmentState state = new AttachmentState();
    state.setPath(path);
    state.setName(name);
    state.setAttachment(attachment);
    attachmentStateRepository.insert(state);
  }

  @Configuration
  @EnableMongoRepositories(basePackageClasses = AttachmentRepository.class,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
      classes = { AttachmentRepository.class, AttachmentStateRepository.class }))
  static class Config extends AbstractMongoClientConfiguration {

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
