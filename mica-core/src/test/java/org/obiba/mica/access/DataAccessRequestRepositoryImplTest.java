/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import jakarta.inject.Inject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.config.MongoDbConfiguration;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.FileStoreService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ContextConfiguration(classes = DataAccessRequestRepositoryImplTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DataAccessRequestRepositoryImplTest {

  @Inject
  private DataAccessRequestRepositoryImpl dataAccessRequestRepository;

  @Inject
  private AttachmentRepository attachmentRepository;

  @Inject
  private FileStoreService fileStoreService;

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
    reset(fileStoreService);
  }

  /**
   * DataAccessRequestService.delete relies on deleteWithReferences alone to delete the attachments: it must remove
   * every attachment of the request (documents and stored files), wherever below the request path it was saved.
   */
  @Test
  public void test_deleteWithReferences_removes_all_attachments_of_the_request_only() {
    DataAccessRequest request = request("111111",
      attachment("a1", null),
      attachment("a2", "/data-access-request/111111/sub"),
      attachment("a3", "/somewhere/else")); // not below the request path: saveAttachments moves it there
    DataAccessRequest other = request("222222", attachment("b1", null));
    // as when uploaded: the attachments are inserted before the request is saved
    request.getAttachments().forEach(attachmentRepository::insert);
    other.getAttachments().forEach(attachmentRepository::insert);
    dataAccessRequestRepository.insertWithReferences(request);
    dataAccessRequestRepository.insertWithReferences(other);
    assertThat(attachmentRepository.count()).isEqualTo(4);

    dataAccessRequestRepository.deleteWithReferences(request);

    assertThat(mongoTemplate.findById("111111", DataAccessRequest.class)).isNull();
    assertThat(mongoTemplate.findById("222222", DataAccessRequest.class)).isNotNull();
    assertThat(attachmentRepository.findAll()).extracting(Attachment::getId).containsExactly("b1");
    verify(fileStoreService).delete("file-a1");
    verify(fileStoreService).delete("file-a2");
    verify(fileStoreService).delete("file-a3");
    verify(fileStoreService, never()).delete("file-b1");
  }

  /**
   * The request path is a literal prefix ending at a path boundary: the attachments of a request whose id starts
   * with the deleted one, or matches it once '.' is read as a regex wildcard, are kept.
   */
  @Test
  public void test_deleteWithReferences_keeps_attachments_of_sibling_ids() {
    DataAccessRequest request = request("DAR.1", attachment("a1", null), attachment("a2", "/data-access-request/DAR.1/sub"));
    DataAccessRequest prefixed = request("DAR.10", attachment("b1", null));
    DataAccessRequest wildcard = request("DARX1", attachment("c1", null));
    for (DataAccessRequest r : Lists.newArrayList(request, prefixed, wildcard)) {
      r.getAttachments().forEach(attachmentRepository::insert);
      dataAccessRequestRepository.insertWithReferences(r);
    }

    dataAccessRequestRepository.deleteWithReferences(request);

    assertThat(attachmentRepository.findAll()).extracting(Attachment::getId).containsExactlyInAnyOrder("b1", "c1");
    verify(fileStoreService).delete("file-a1");
    verify(fileStoreService).delete("file-a2");
    verify(fileStoreService, never()).delete("file-b1");
    verify(fileStoreService, never()).delete("file-c1");
  }

  private static DataAccessRequest request(String id, Attachment... attachments) {
    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder().applicant("applicant").build();
    request.setId(id);
    request.setAttachments(Lists.newArrayList(attachments));
    return request;
  }

  private static Attachment attachment(String id, String path) {
    Attachment attachment = new Attachment();
    attachment.setId(id);
    attachment.setName(id + ".pdf");
    attachment.setFileReference("file-" + id);
    attachment.setPath(path);
    return attachment;
  }

  @Configuration
  @EnableMongoRepositories(basePackageClasses = AttachmentRepository.class,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AttachmentRepository.class))
  static class Config extends AbstractMongoClientConfiguration {

    @Bean
    public FileStoreService fileStoreService() {
      return mock(FileStoreService.class);
    }

    @Bean
    public DataAccessRequestRepositoryImpl dataAccessRequestRepository(AttachmentRepository attachmentRepository,
      FileStoreService fileStoreService, MongoTemplate mongoTemplate) {
      // not a bean: Spring would try to inject the fields of the mocked service
      return new DataAccessRequestRepositoryImpl(attachmentRepository, fileStoreService, mongoTemplate,
        mock(VariableSetService.class));
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
