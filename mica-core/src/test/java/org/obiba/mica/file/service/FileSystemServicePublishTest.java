/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.obiba.mica.AbstractShiroTest;
import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.event.DatasetPublishedEvent;
import org.obiba.mica.dataset.event.DatasetUnpublishedEvent;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.notification.FilePublicationFlowMailNotification;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.event.NetworkPublishedEvent;
import org.obiba.mica.network.event.NetworkUnpublishedEvent;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.ProjectPublishedEvent;
import org.obiba.mica.project.event.ProjectUnpublishedEvent;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.obiba.mica.study.event.StudyUnpublishedEvent;
import org.springframework.test.util.ReflectionTestUtils;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The publication from the file browser is notified, the one cascaded from a document (study, network etc.) is not, as
 * the document publication is.
 */
public class FileSystemServicePublishTest extends AbstractShiroTest {

  private FileSystemService fileSystemService;

  private FilePublicationFlowMailNotification notification;

  private EventBus eventBus;

  // in-memory attachment states, queried by the repository mock
  private final List<AttachmentState> states = new ArrayList<>();

  @BeforeEach
  public void setUp() {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("editor");
    setSubject(subject);

    AttachmentStateRepository attachmentStateRepository = mock(AttachmentStateRepository.class);
    when(attachmentStateRepository.findByPath(anyString())).thenAnswer(i -> {
      Pattern pathRegEx = Pattern.compile(i.getArgument(0));
      return states.stream().filter(s -> pathRegEx.matcher(s.getPath()).find()).collect(toList());
    });
    when(attachmentStateRepository.findByPathAndName(anyString(), anyString())).thenAnswer(i -> states.stream()
      .filter(s -> s.getPath().equals(i.getArgument(0)) && s.getName().equals(i.getArgument(1)))
      .collect(toList()));
    when(attachmentStateRepository.save(any(AttachmentState.class))).thenAnswer(i -> i.getArgument(0));

    notification = mock(FilePublicationFlowMailNotification.class);
    eventBus = mock(EventBus.class);

    fileSystemService = new FileSystemService();
    ReflectionTestUtils.setField(fileSystemService, "attachmentStateRepository", attachmentStateRepository);
    ReflectionTestUtils.setField(fileSystemService, "filePublicationFlowNotification", notification);
    ReflectionTestUtils.setField(fileSystemService, "eventBus", eventBus);
  }

  @AfterEach
  public void tearDown() {
    clearSubject();
  }

  @Test
  public void test_publish_file_is_notified() {
    AttachmentState file = createState("/network/n1", "doc.pdf");

    fileSystemService.publish("/network/n1", "doc.pdf", true);

    assertThat(file.isPublished()).isTrue();
    verify(notification).sendPublished("/network/n1", "doc.pdf", true);
  }

  @Test
  public void test_unpublish_file_is_notified() {
    AttachmentState file = createPublishedState("/network/n1", "doc.pdf");

    fileSystemService.publish("/network/n1", "doc.pdf", false);

    assertThat(file.isPublished()).isFalse();
    verify(notification).sendPublished("/network/n1", "doc.pdf", false);
  }

  @Test
  public void test_publish_folder_is_notified() {
    createFolder("/network/n1/docs");
    AttachmentState file = createState("/network/n1/docs", "doc.pdf");

    fileSystemService.publish("/network/n1/docs", true);

    assertThat(file.isPublished()).isTrue();
    verify(notification).sendPublished("/network/n1/docs", true);
  }

  @Test
  public void test_unpublish_folder_is_notified() {
    createFolder("/network/n1/docs");
    AttachmentState file = createPublishedState("/network/n1/docs", "doc.pdf");

    fileSystemService.publish("/network/n1/docs", false);

    assertThat(file.isPublished()).isFalse();
    verify(notification).sendPublished("/network/n1/docs", false);
  }

  @Test
  public void test_publish_file_succeeds_when_notification_fails() {
    AttachmentState file = createState("/network/n1", "doc.pdf");
    doThrow(new RuntimeException("mail failure")).when(notification).sendPublished(anyString(), anyString(), anyBoolean());

    fileSystemService.publish("/network/n1", "doc.pdf", true);

    assertThat(file.isPublished()).isTrue();
  }

  @Test
  public void test_publish_folder_succeeds_when_notification_fails() {
    createFolder("/network/n1/docs");
    AttachmentState file = createState("/network/n1/docs", "doc.pdf");
    doThrow(new RuntimeException("mail failure")).when(notification).sendPublished(anyString(), anyBoolean());

    fileSystemService.publish("/network/n1/docs", true);

    assertThat(file.isPublished()).isTrue();
  }

  //
  // Publication cascaded from the documents
  //

  @Test
  public void test_study_publication_cascade_is_not_notified() {
    AttachmentState file = createState("/individual-study/s1", "doc.pdf");

    fileSystemService.studyPublished(new StudyPublishedEvent(study("s1"), "editor", PublishCascadingScope.ALL));

    assertThat(file.isPublished()).isTrue();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_study_under_review_publication_cascade_is_not_notified() {
    AttachmentState file = createState("/individual-study/s1", "doc.pdf");
    file.setRevisionStatus(RevisionStatus.UNDER_REVIEW);

    fileSystemService.studyPublished(new StudyPublishedEvent(study("s1"), "editor", PublishCascadingScope.UNDER_REVIEW));

    assertThat(file.isPublished()).isTrue();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_study_unpublication_cascade_is_not_notified() {
    AttachmentState file = createPublishedState("/individual-study/s1", "doc.pdf");

    fileSystemService.studyUnpublished(new StudyUnpublishedEvent(study("s1")));

    assertThat(file.isPublished()).isFalse();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_network_publication_cascade_is_not_notified() {
    AttachmentState file = createState("/network/n1", "doc.pdf");

    fileSystemService.networkPublished(new NetworkPublishedEvent(network("n1"), "editor", PublishCascadingScope.ALL));

    assertThat(file.isPublished()).isTrue();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_network_unpublication_cascade_is_not_notified() {
    AttachmentState file = createPublishedState("/network/n1", "doc.pdf");

    fileSystemService.networkUnpublished(new NetworkUnpublishedEvent(network("n1")));

    assertThat(file.isPublished()).isFalse();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_collected_dataset_publication_cascade_is_not_notified() {
    AttachmentState file = createState("/collected-dataset/ds1", "doc.pdf");
    StudyDataset dataset = new StudyDataset();
    dataset.setId("ds1");

    fileSystemService.datasetPublished(new DatasetPublishedEvent(dataset, null, "editor", PublishCascadingScope.ALL));

    assertThat(file.isPublished()).isTrue();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_harmonized_dataset_unpublication_cascade_is_not_notified() {
    AttachmentState file = createPublishedState("/harmonized-dataset/ds1", "doc.pdf");
    HarmonizationDataset dataset = new HarmonizationDataset();
    dataset.setId("ds1");

    fileSystemService.datasetUnpublished(new DatasetUnpublishedEvent(dataset));

    assertThat(file.isPublished()).isFalse();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_project_publication_cascade_is_not_notified() {
    AttachmentState file = createState("/project/p1", "doc.pdf");

    fileSystemService.projectPublished(new ProjectPublishedEvent(project("p1"), "editor", PublishCascadingScope.ALL));

    assertThat(file.isPublished()).isTrue();
    verifyNoInteractions(notification);
  }

  @Test
  public void test_project_unpublication_cascade_is_not_notified() {
    AttachmentState file = createPublishedState("/project/p1", "doc.pdf");

    fileSystemService.projectUnpublished(new ProjectUnpublishedEvent(project("p1")));

    assertThat(file.isPublished()).isFalse();
    verifyNoInteractions(notification);
  }

  //
  // Private methods
  //

  private Study study(String id) {
    Study study = new Study();
    study.setId(id);
    return study;
  }

  private Network network(String id) {
    Network network = new Network();
    network.setId(id);
    return network;
  }

  private Project project(String id) {
    Project project = new Project();
    project.setId(id);
    return project;
  }

  private void createFolder(String path) {
    createState(path, FileSystemService.DIR_NAME);
  }

  private AttachmentState createPublishedState(String path, String name) {
    AttachmentState state = createState(path, name);
    state.publish("editor");
    return state;
  }

  private AttachmentState createState(String path, String name) {
    Attachment attachment = new Attachment();
    attachment.setId(new ObjectId().toString());
    attachment.setPath(path);
    attachment.setName(name);
    AttachmentState state = new AttachmentState();
    state.setPath(path);
    state.setName(name);
    state.setAttachment(attachment);
    states.add(state);
    return state;
  }
}
