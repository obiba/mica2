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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.obiba.mica.config.JerseyConfiguration;
import org.obiba.shiro.web.filter.AuthenticationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/auth")
public class SessionsResource {

  private static final Logger log = LoggerFactory.getLogger(SessionsResource.class);

  @Inject
  private AuthenticationExecutor authenticationExecutor;

  @POST
  @Path("/sessions")
  public Response createSession(@SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest servletRequest,
      @FormParam("username") String username, @FormParam("password") String password) {
    try {
      authenticationExecutor.login(new UsernamePasswordToken(username, password));
      String sessionId = SecurityUtils.getSubject().getSession().getId().toString();
      log.info("Successful session creation for user '{}' session ID is '{}'.", username, sessionId);
      return Response
          .created(UriBuilder.fromPath(JerseyConfiguration.WS_ROOT).path(SessionResource.class).build(sessionId))
          .build();

    } catch(AuthenticationException e) {
      log.info("Authentication failure of user '{}' at ip: '{}': {}", username, servletRequest.getRemoteAddr(),
          e.getMessage());
      // When a request contains credentials and they are invalid, the a 403 (Forbidden) should be returned.
      return Response.status(Response.Status.FORBIDDEN).cookie().build();
    }
  }
}

