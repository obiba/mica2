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

import java.io.IOException;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;

@Priority(Integer.MIN_VALUE)
public class AuthenticationInterceptor implements ContainerResponseFilter {

  private static final String MICA_SESSION_ID_COOKIE_NAME = "micasid";

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    // Set the cookie if the user is still authenticated
    if(isUserAuthenticated()) {
      Session session = SecurityUtils.getSubject().getSession();
      session.touch();
      int timeout = (int) (session.getTimeout() / 1000);
      NewCookie sidCookie = new NewCookie(MICA_SESSION_ID_COOKIE_NAME, session.getId().toString(), "/", null, null, timeout, false);
      MultivaluedMap<String, Object> headers = responseContext.getHeaders();
      List<Object> cookies = headers.get(HttpHeaders.SET_COOKIE);
      if (cookies == null)
        headers.putSingle(HttpHeaders.SET_COOKIE, sidCookie);
      else
        headers.add(HttpHeaders.SET_COOKIE, sidCookie);
      Object cookieValue = session.getAttribute(HttpHeaders.SET_COOKIE);
      if(cookieValue != null) {
        headers.add(HttpHeaders.SET_COOKIE, NewCookie.valueOf(cookieValue.toString()));
      }
    } else {
      if(responseContext.getHeaders().get(HttpHeaders.SET_COOKIE) == null) {
        responseContext.getHeaders().putSingle(HttpHeaders.SET_COOKIE,
            new NewCookie(MICA_SESSION_ID_COOKIE_NAME, null, "/", null, "Mica session deleted", 0, false));
      }
    }
  }

  private boolean isUserAuthenticated() {
    return SecurityUtils.getSubject().isAuthenticated();
  }

}
