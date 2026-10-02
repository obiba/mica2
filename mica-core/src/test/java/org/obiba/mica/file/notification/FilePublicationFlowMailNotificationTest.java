/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.MicaGroupsToRolesMapper;
import org.obiba.mica.security.service.SubjectAclService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FilePublicationFlowMailNotificationTest {

  @InjectMocks
  private FilePublicationFlowMailNotification notification;

  @Mock
  private MicaConfigService micaConfigService;

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private MailService mailService;

  @Mock
  private MicaGroupsToRolesMapper groupsToRolesMapper;

  @Mock
  private MicaConfig micaConfig;

  @BeforeEach
  public void init() {
    when(micaConfigService.getConfig()).thenReturn(micaConfig);
    when(micaConfig.getName()).thenReturn("Mica");
    when(micaConfig.isFsNotificationsEnabled()).thenReturn(true);
    when(groupsToRolesMapper.toGroups(Roles.MICA_REVIEWER)).thenReturn(Collections.singleton("agate-reviewer"));
    when(groupsToRolesMapper.toGroups(Roles.MICA_EDITOR)).thenReturn(Collections.singleton("agate-editor"));
    when(subjectAclService.findByResourceInstance(anyString(), anyString())).thenReturn(new ArrayList<>());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testStatusChangeContextContainsStatus() {
    notification.send("/network/network-1/doc.pdf", RevisionStatus.DRAFT, RevisionStatus.UNDER_REVIEW);

    ArgumentCaptor<Map<String, String>> ctx = ArgumentCaptor.forClass(Map.class);
    verify(mailService).sendEmailToGroupsAndUsers(any(), eq(FilePublicationFlowMailNotification.FILE_NOTIFICATION_TEMPLATE),
      ctx.capture(), anyCollection(), anyCollection());

    assertEquals("UNDER_REVIEW", ctx.getValue().get("status"));
    assertEquals("/network/network-1", ctx.getValue().get("document"));
  }

  @Test
  public void testFileStatusChangeContextContainsFilePath() {
    notification.send("/harmonized-dataset/ds-1", "data.csv", RevisionStatus.DRAFT, RevisionStatus.UNDER_REVIEW);

    Map<String, String> ctx = captureContext();
    assertEquals("/harmonized-dataset/ds-1/data.csv", ctx.get("path"));
    assertEquals("/harmonized-dataset/ds-1", ctx.get("document"));
    assertEquals("ds-1", ctx.get("documentId"));
  }

  @Test
  public void testFolderStatusChangeContextContainsFolderPath() {
    notification.send("/harmonized-dataset/ds-1/docs", RevisionStatus.DRAFT, RevisionStatus.UNDER_REVIEW);

    Map<String, String> ctx = captureContext();
    assertEquals("/harmonized-dataset/ds-1/docs", ctx.get("path"));
    assertEquals("/harmonized-dataset/ds-1", ctx.get("document"));
  }

  @Test
  public void testNoNotificationForFileAtTheRootOfADocumentType() {
    notification.send("/network", "doc.pdf", RevisionStatus.DRAFT, RevisionStatus.UNDER_REVIEW);

    verify(mailService, never()).sendEmailToGroupsAndUsers(any(), any(), any(), anyCollection(), anyCollection());
  }

  @Test
  public void testFilePublishedContext() {
    notification.sendPublished("/harmonized-dataset/ds-1", "data.csv", true);

    Map<String, String> ctx = captureContext(FilePublicationFlowMailNotification.FILE_PUBLISHED_TEMPLATE);
    assertEquals("published", ctx.get("published"));
    assertEquals("PUBLISHED", ctx.get("status"));
    assertEquals("/harmonized-dataset/ds-1/data.csv", ctx.get("path"));
    assertEquals("/harmonized-dataset/ds-1", ctx.get("document"));
  }

  @Test
  public void testFolderUnpublishedContext() {
    notification.sendPublished("/harmonized-dataset/ds-1/docs", false);

    Map<String, String> ctx = captureContext(FilePublicationFlowMailNotification.FILE_PUBLISHED_TEMPLATE);
    assertEquals("unpublished", ctx.get("published"));
    assertEquals("UNPUBLISHED", ctx.get("status"));
    assertEquals("/harmonized-dataset/ds-1/docs", ctx.get("path"));
  }

  @Test
  public void testPublishedRecipientsAreSubjectsAllowedToEdit() {
    when(subjectAclService.findByResourceInstance("/draft/file", "/network/network-1")).thenReturn(new ArrayList<>(List.of(
      SubjectAcl.newBuilder("file-editor", SubjectAcl.Type.USER).action("VIEW", "EDIT").build(),
      SubjectAcl.newBuilder("file-reader", SubjectAcl.Type.USER).action("VIEW").build())));

    notification.sendPublished("/network/network-1", "doc.pdf", true);

    verify(mailService).sendEmailToGroupsAndUsers(any(), eq(FilePublicationFlowMailNotification.FILE_PUBLISHED_TEMPLATE),
      any(), eq(List.of("agate-reviewer", "agate-editor")), eq(List.of("file-editor")));
  }

  @Test
  public void testNoPublishedNotificationWhenDisabled() {
    when(micaConfig.isFsNotificationsEnabled()).thenReturn(false);

    notification.sendPublished("/network/network-1", "doc.pdf", true);

    verify(mailService, never()).sendEmailToGroupsAndUsers(any(), any(), any(), anyCollection(), anyCollection());
  }

  @Test
  public void testNoPublishedNotificationForFileAtTheRootOfADocumentType() {
    notification.sendPublished("/network", "doc.pdf", true);

    verify(mailService, never()).sendEmailToGroupsAndUsers(any(), any(), any(), anyCollection(), anyCollection());
  }

  private Map<String, String> captureContext() {
    return captureContext(FilePublicationFlowMailNotification.FILE_NOTIFICATION_TEMPLATE);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> captureContext(String template) {
    ArgumentCaptor<Map<String, String>> ctx = ArgumentCaptor.forClass(Map.class);
    verify(mailService).sendEmailToGroupsAndUsers(any(), eq(template), ctx.capture(), anyCollection(), anyCollection());
    return ctx.getValue();
  }
}
