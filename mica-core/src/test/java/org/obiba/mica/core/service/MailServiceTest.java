package org.obiba.mica.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MailServiceTest {

  private RestTemplate restTemplate;

  private MailService mailService;

  @BeforeEach
  public void setUp() {
    AgateServerConfigService agateServerConfigService = mock(AgateServerConfigService.class);
    when(agateServerConfigService.getAgateUri()).thenReturn(URI.create("https://agate.example.org"));

    restTemplate = mock(RestTemplate.class);
    when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
      .thenReturn(ResponseEntity.noContent().build());

    mailService = spy(new MailService());
    ReflectionTestUtils.setField(mailService, "agateServerConfigService", agateServerConfigService);
    doReturn(restTemplate).when(mailService).newRestTemplate();
    doReturn("Basic token").when(mailService).getApplicationAuth();
  }

  @Test
  public void testNoRecipientIsNotSentToAgate() {
    // without any user or group, Agate would notify all the users of the application
    mailService.sendEmailToGroupsAndUsers("subject", "template", Map.of(), List.of(), List.of());
    mailService.sendEmailToUsers("subject", "template", Map.of());
    mailService.sendEmailToGroups("subject", "template", Map.of());
    mailService.sendEmailToUsers("subject", "text");
    mailService.sendEmailToGroups("subject", "text");

    verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
  }

  @Test
  public void testGroupRecipientIsSentToAgate() {
    mailService.sendEmailToGroupsAndUsers("subject", "template", Map.of(), List.of("dac"), List.of());

    ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).exchange(eq("https://agate.example.org/ws/notifications"), eq(HttpMethod.POST), entity.capture(),
      eq(String.class));
    assertTrue(((String) entity.getValue().getBody()).startsWith("group=dac&subject=subject"));
  }
}
