package org.obiba.mica.core.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.MicaGroupsToRolesMapper;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommentMailNotificationTest {

  private MailService mailService;

  private CommentMailNotification notification;

  @BeforeEach
  public void setUp() {
    mailService = mock(MailService.class);
    when(mailService.getSubject(any(), any(), anyString())).thenReturn("subject");

    MicaConfig config = new MicaConfig();
    config.setCommentNotificationsEnabled(true);
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getConfig()).thenReturn(config);
    when(micaConfigService.getPublicUrl()).thenReturn("https://mica.example.org");

    SubjectAclService subjectAclService = mock(SubjectAclService.class);
    when(subjectAclService.findByResourceInstance(anyString(), anyString())).thenReturn(Collections.emptyList());

    MicaGroupsToRolesMapper groupsToRolesMapper = new MicaGroupsToRolesMapper(new MockEnvironment()
      .withProperty("roles.mica-reviewer", "mica-reviewer-a")
      .withProperty("roles.mica-editor", "mica-editor-a|mica-editor-b"));

    notification = new CommentMailNotification();
    ReflectionTestUtils.setField(notification, "mailService", mailService);
    ReflectionTestUtils.setField(notification, "micaConfigService", micaConfigService);
    ReflectionTestUtils.setField(notification, "subjectAclService", subjectAclService);
    ReflectionTestUtils.setField(notification, "groupsToRolesMapper", groupsToRolesMapper);
  }

  @Test
  public void testNotifiesMappedGroups() {
    Comment comment = Comment.newBuilder().createdBy("someone").message("hello")
      .resourceId("/draft/individual-study").instanceId("study-1").build();

    notification.send(comment);

    ArgumentCaptor<Collection<String>> groups = ArgumentCaptor.forClass(Collection.class);
    ArgumentCaptor<Collection<String>> users = ArgumentCaptor.forClass(Collection.class);
    verify(mailService).sendEmailToGroupsAndUsers(anyString(), anyString(), any(Map.class), groups.capture(), users.capture());
    // the Agate groups granting the roles are notified, not the role names
    assertEquals(Set.of("mica-reviewer-a", "mica-editor-a", "mica-editor-b"), Set.copyOf(groups.getValue()));
    assertEquals(Collections.emptyList(), users.getValue());
  }
}
