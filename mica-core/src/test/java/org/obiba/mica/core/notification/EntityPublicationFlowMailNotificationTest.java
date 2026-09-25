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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.obiba.mica.security.service.MicaGroupsToRolesMapper;
import org.obiba.mica.security.service.SubjectAclService;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EntityPublicationFlowMailNotificationTest {

  @InjectMocks
  private EntityPublicationFlowMailNotification notification;

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
    when(subjectAclService.findByResourceInstance(anyString(), anyString())).thenReturn(new java.util.ArrayList<>());
    when(micaConfig.getStudyNotificationsSubject()).thenReturn("STUDY-SUBJECT");
    when(micaConfig.getNetworkNotificationsSubject()).thenReturn("NETWORK-SUBJECT");
  }

  @Test
  public void testStatusChangeUsesSubjectOfItsOwnType() {
    when(micaConfig.isNetworkNotificationsEnabled()).thenReturn(true);

    notification.send("network-1", "network", RevisionStatus.DRAFT, RevisionStatus.UNDER_REVIEW);

    verify(mailService).getSubject(eq("NETWORK-SUBJECT"), anyMap(), anyString());
  }

  @Test
  public void testStudyStatusChangeUsesStudySubject() {
    when(micaConfig.isStudyNotificationsEnabled()).thenReturn(true);

    notification.send("study-1", "harmonization-study", RevisionStatus.DRAFT, RevisionStatus.UNDER_REVIEW);

    verify(mailService).getSubject(eq("STUDY-SUBJECT"), anyMap(), anyString());
  }
}
