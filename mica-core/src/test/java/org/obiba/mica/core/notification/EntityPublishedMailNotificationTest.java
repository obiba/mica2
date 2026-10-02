/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.MicaGroupsToRolesMapper;
import org.obiba.mica.security.service.SubjectAclService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EntityPublishedMailNotificationTest {

  @InjectMocks
  private EntityPublishedMailNotification notification;

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
    when(groupsToRolesMapper.toGroups(Roles.MICA_REVIEWER)).thenReturn(Collections.singleton("agate-reviewer"));
    when(groupsToRolesMapper.toGroups(Roles.MICA_EDITOR)).thenReturn(Collections.singleton("agate-editor"));
    // a new list per call, as the notification appends the type-wide ACLs to the instance ones
    when(subjectAclService.findByResourceInstance(anyString(), anyString())).thenAnswer(i -> new ArrayList<>());
    when(mailService.getSubject(anyString(), anyMap(), anyString())).thenReturn("SUBJECT");

    when(micaConfig.isStudyNotificationsEnabled()).thenReturn(true);
    when(micaConfig.isNetworkNotificationsEnabled()).thenReturn(true);
    when(micaConfig.isStudyDatasetNotificationsEnabled()).thenReturn(true);
    when(micaConfig.isHarmonizationDatasetNotificationsEnabled()).thenReturn(true);
    when(micaConfig.isProjectNotificationsEnabled()).thenReturn(true);

    when(micaConfig.getStudyNotificationsSubject()).thenReturn("STUDY-SUBJECT");
    when(micaConfig.getNetworkNotificationsSubject()).thenReturn("NETWORK-SUBJECT");
    when(micaConfig.getStudyDatasetNotificationsSubject()).thenReturn("COLLECTED-DATASET-SUBJECT");
    when(micaConfig.getHarmonizationDatasetNotificationsSubject()).thenReturn("HARMONIZED-DATASET-SUBJECT");
    when(micaConfig.getProjectNotificationsSubject()).thenReturn("PROJECT-SUBJECT");
  }

  @ParameterizedTest
  @CsvSource({
    "individual-study, STUDY-SUBJECT, Individual Study",
    "harmonization-study, STUDY-SUBJECT, Harmonization Study",
    "network, NETWORK-SUBJECT, Network",
    "collected-dataset, COLLECTED-DATASET-SUBJECT, Collected Dataset",
    "harmonized-dataset, HARMONIZED-DATASET-SUBJECT, Harmonized Dataset",
    "project, PROJECT-SUBJECT, Project"
  })
  public void testPublishedUsesSubjectAndTemplateOfItsOwnType(String typeName, String expectedSubject,
    String expectedTitle) {
    notification.send("entity-1", typeName, true);

    verify(mailService).getSubject(eq(expectedSubject), anyMap(),
      eq("[${organization}] ${documentId}: " + expectedTitle + " has been ${published}"));
    verify(mailService).sendEmailToGroupsAndUsers(eq("SUBJECT"), eq(typeName + "Published"), anyMap(), anyList(),
      anyList());
  }

  @Test
  public void testPublishedContext() {
    notification.send("entity-1", "network", true);

    Map<String, String> ctx = captureContext();
    assertThat(ctx).containsEntry("published", "published")
      .containsEntry("status", "PUBLISHED")
      .containsEntry("documentType", "network")
      .containsEntry("documentId", "entity-1")
      .containsEntry("organization", "Mica");
  }

  @Test
  public void testUnpublishedContext() {
    notification.send("entity-1", "network", false);

    assertThat(captureContext()).containsEntry("published", "unpublished")
      .containsEntry("status", "UNPUBLISHED");
  }

  @Test
  public void testRecipientsAreRoleGroupsAndSubjectsAllowedToEdit() {
    when(subjectAclService.findByResourceInstance("/draft/network", "entity-1")).thenAnswer(i -> new ArrayList<>(List.of(
      SubjectAcl.newBuilder("instance-editor", SubjectAcl.Type.USER).action("VIEW", "EDIT").build(),
      SubjectAcl.newBuilder("instance-reader", SubjectAcl.Type.USER).action("VIEW").build())));
    when(subjectAclService.findByResourceInstance("/draft/network", "*")).thenAnswer(i -> new ArrayList<>(List.of(
      SubjectAcl.newBuilder("type-editor", SubjectAcl.Type.USER).action("VIEW", "EDIT").build(),
      SubjectAcl.newBuilder("type-group", SubjectAcl.Type.GROUP).action("VIEW", "EDIT").build(),
      SubjectAcl.newBuilder("type-reader-group", SubjectAcl.Type.GROUP).action("VIEW").build())));

    notification.send("entity-1", "network", true);

    verify(mailService).sendEmailToGroupsAndUsers(anyString(), anyString(), anyMap(),
      eq(List.of("agate-reviewer", "agate-editor", "type-group")),
      eq(List.of("instance-editor", "type-editor")));
  }

  @Test
  public void testNoNotificationWhenDisabledForType() {
    when(micaConfig.isNetworkNotificationsEnabled()).thenReturn(false);

    notification.send("entity-1", "network", true);

    verifyNoInteractions(subjectAclService, mailService);
  }

  @Test
  public void testNoNotificationWithoutRecipients() {
    when(groupsToRolesMapper.toGroups(anyString())).thenReturn(Collections.emptySet());

    notification.send("entity-1", "network", true);

    verify(mailService, never()).sendEmailToGroupsAndUsers(anyString(), anyString(), anyMap(), anyList(), anyList());
  }

  @Test
  public void testUnknownTypeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> notification.send("entity-1", "unknown", true));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> captureContext() {
    ArgumentCaptor<Map<String, String>> ctx = ArgumentCaptor.forClass(Map.class);
    verify(mailService).sendEmailToGroupsAndUsers(anyString(), anyString(), ctx.capture(), any(), any());
    return ctx.getValue();
  }
}
