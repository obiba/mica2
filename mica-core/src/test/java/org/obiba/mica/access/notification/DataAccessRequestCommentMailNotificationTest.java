package org.obiba.mica.access.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.MicaGroupsToRolesMapper;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataAccessRequestCommentMailNotificationTest {

  private MailService mailService;

  private SubjectAclService subjectAclService;

  private DataAccessRequestCommentMailNotification notification;

  @BeforeEach
  public void setUp() {
    mailService = mock(MailService.class);
    // real subject resolution: the default subject applies when none is configured
    when(mailService.getSubject(any(), any(), anyString())).thenCallRealMethod();

    DataAccessConfig dataAccessConfig = new DataAccessConfig();
    dataAccessConfig.setNotifyCommented(true);
    DataAccessConfigService dataAccessConfigService = mock(DataAccessConfigService.class);
    when(dataAccessConfigService.getOrCreateConfig()).thenReturn(dataAccessConfig);

    DataAccessRequest request = (DataAccessRequest) DataAccessRequest.newBuilder()
      .applicant("applicant").status("SUBMITTED").build();
    request.setId("dar-1");
    DataAccessRequestService dataAccessRequestService = mock(DataAccessRequestService.class);
    when(dataAccessRequestService.findById("dar-1")).thenReturn(request);

    DataAccessRequestUtilService dataAccessRequestUtilService = mock(DataAccessRequestUtilService.class);
    when(dataAccessRequestUtilService.getRequestTitle(request)).thenReturn("My Project");

    subjectAclService = mock(SubjectAclService.class);
    when(subjectAclService.findByResourceInstance("/data-access-request/private-comment", "*")).thenReturn(List.of(
      SubjectAcl.newBuilder("dac", SubjectAcl.Type.GROUP).resource("/data-access-request/private-comment").instance("*").build()));

    MicaConfig config = new MicaConfig();
    config.setName("Mica A");
    MicaConfigService micaConfigService = mock(MicaConfigService.class);
    when(micaConfigService.getConfig()).thenReturn(config);
    when(micaConfigService.getPublicUrl()).thenReturn("https://mica.example.org");

    MicaGroupsToRolesMapper groupsToRolesMapper = new MicaGroupsToRolesMapper(new MockEnvironment()
      .withProperty("roles.mica-data-access-officer", "dao-a"));

    notification = new DataAccessRequestCommentMailNotification(dataAccessConfigService, dataAccessRequestUtilService,
      dataAccessRequestService, mailService, subjectAclService, micaConfigService, groupsToRolesMapper);
  }

  @Test
  public void testPrivateCommentNotifiesPrivateCommentReadersWithDefaultSubject() {
    Comment comment = Comment.newBuilder().createdBy("dao").message("hello")
      .resourceId("/data-access-request").instanceId("dar-1").admin(true).build();

    notification.send(comment);

    ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Collection<String>> groups = ArgumentCaptor.forClass(Collection.class);
    ArgumentCaptor<Collection<String>> users = ArgumentCaptor.forClass(Collection.class);
    verify(mailService).sendEmailToGroupsAndUsers(subject.capture(), eq("dataAccessRequestCommentAdded"), any(Map.class),
      groups.capture(), users.capture());
    // no commented subject configured: the default one applies, a null subject would fail the email
    assertEquals("[Mica A] My Project", subject.getValue());
    assertEquals(List.of("dac"), List.copyOf(groups.getValue()));
    assertEquals(Collections.emptyList(), users.getValue());

    verify(mailService).sendEmailToGroups(eq("[Mica A] My Project"), eq("dataAccessRequestCommentAdded"), any(Map.class),
      eq("dao-a"));
  }

  @Test
  public void testPrivateCommentWithoutPrivateCommentReadersNotifiesDaoOnly() {
    when(subjectAclService.findByResourceInstance("/data-access-request/private-comment", "*")).thenReturn(List.of());
    Comment comment = Comment.newBuilder().createdBy("dao").message("hello")
      .resourceId("/data-access-request").instanceId("dar-1").admin(true).build();

    notification.send(comment);

    // no recipient would make Agate notify all the users of the application
    verify(mailService, never()).sendEmailToGroupsAndUsers(anyString(), anyString(), any(Map.class), any(), any());
    verify(mailService).sendEmailToGroups(eq("[Mica A] My Project"), eq("dataAccessRequestCommentAdded"), any(Map.class),
      eq("dao-a"));
  }
}
