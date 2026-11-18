package org.obiba.mica.core.service;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class UserAuthServiceTest {

  private UserAuthService userAuthService;

  @BeforeEach
  public void setUp() {
    AgateServerConfigService agateServerConfigService = mock(AgateServerConfigService.class);
    when(agateServerConfigService.getAgateUrl()).thenReturn("http://agate:8081");
    userAuthService = spy(new UserAuthService());
    ReflectionTestUtils.setField(userAuthService, "agateServerConfigService", agateServerConfigService);
  }

  @Test
  public void testAgatePublicUrlFromAgate() {
    doReturn(new JSONObject().put("publicUrl", "https://agate.example.org/")).when(userAuthService).getPublicConfiguration();
    assertEquals("https://agate.example.org", userAuthService.getAgatePublicUrl());
  }

  @Test
  public void testAgatePublicUrlFallback() {
    doReturn(new JSONObject()).when(userAuthService).getPublicConfiguration();
    assertEquals("http://agate:8081", userAuthService.getAgatePublicUrl());
  }
}
