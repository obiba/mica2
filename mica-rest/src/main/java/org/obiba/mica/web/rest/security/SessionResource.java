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

import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.springframework.stereotype.Component;

@Component
@Path("/auth/session/{id}")
public class SessionResource  {

  @PathParam("id")
  String sessionId;

  @HEAD
  public Response checkSession() {
    // Only the owner of the session (identified by its cookie) may check that it is still alive:
    // this keeps the endpoint from being used as an oracle for session identifiers.
    return isCurrentSession(sessionId) ? Response.ok().build() : Response.status(Response.Status.NOT_FOUND).build();
  }

  private boolean isCurrentSession(String sessionId) {
    if (sessionId == null) return false;
    try {
      Session current = SecurityUtils.getSubject().getSession(false);
      return current != null && current.getId() != null && sessionId.equals(current.getId().toString());
    } catch (SessionException e) {
      // Means that the session does not exist or has expired.
      return false;
    }
  }
}
