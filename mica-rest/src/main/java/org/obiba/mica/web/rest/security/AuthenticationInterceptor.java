/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Priority(Integer.MIN_VALUE)
@Component
public class AuthenticationInterceptor implements ContainerResponseFilter {

  private static final String MICA_SESSION_ID_COOKIE_NAME = "micasid";

  private final MicaConfigService micaConfigService;

  private final CSRFTokenHelper csrfTokenHelper;

  @Inject
  public AuthenticationInterceptor(MicaConfigService micaConfigService, CSRFTokenHelper csrfTokenHelper) {
    this.micaConfigService = micaConfigService;
    this.csrfTokenHelper = csrfTokenHelper;
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    // Set the cookie if the user is still authenticated
    String path = micaConfigService.getContextPath() + "/";
    if (isUserAuthenticated()) {
      Session session = SecurityUtils.getSubject().getSession();
      session.touch();
      int timeout = (int) (session.getTimeout() / 1000);
      responseContext.getHeaders().add(HttpHeaders.SET_COOKIE,
        new NewCookie.Builder(MICA_SESSION_ID_COOKIE_NAME)
          .value(session.getId().toString())
          .path(path)
          .maxAge(timeout)
          .secure(true)
          .httpOnly(true)
          .sameSite(NewCookie.SameSite.LAX)
          .build());
      NewCookie csrfCookie = csrfTokenHelper.createCsrfTokenCookie();
      if(csrfCookie != null)
        responseContext.getHeaders().add(HttpHeaders.SET_COOKIE, csrfCookie);
    } else {
      if (responseContext.getHeaders().get(HttpHeaders.SET_COOKIE) == null) {
        responseContext.getHeaders().putSingle(HttpHeaders.SET_COOKIE,
          new NewCookie.Builder(MICA_SESSION_ID_COOKIE_NAME)
            .path(path)
            .comment("Mica session deleted")
            .maxAge(0)
            .secure(true)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.LAX)
            .build());
        responseContext.getHeaders().add(HttpHeaders.SET_COOKIE, csrfTokenHelper.deleteCsrfTokenCookie());
      }
    }
  }

  private boolean isUserAuthenticated() {
    return SecurityUtils.getSubject().isAuthenticated();
  }

}
