/*******************************************************************************
 * Copyright 2014(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.mica.web.rest.security;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/auth/session/_current")
@RequiresAuthentication
public class CurrentSessionResource {

  private static final String OBIBA_ID_COOKIE_NAME = "obibaid";

  @DELETE
  public Response deleteSession() {
    // Delete the Shiro session
    try {
      SecurityUtils.getSubject().logout();
      return Response.ok().header(HttpHeaders.SET_COOKIE,
          new NewCookie(OBIBA_ID_COOKIE_NAME, null, "/", null, "Obiba session deleted", 0, false)).build();
    } catch(InvalidSessionException e) {
      // Ignore
      return Response.ok().build();
    }
  }

  @GET
  public Mica.SessionDto get() {
    Subject subject = SecurityUtils.getSubject();
    Mica.SessionDto.Builder builder = Mica.SessionDto.newBuilder() //
        .setUsername(subject.getPrincipal().toString());

    try {
      if (subject.hasRole(Roles.MICA_ADMIN)) {
        builder.setRole(Roles.MICA_ADMIN);
      } else if (subject.hasRole(Roles.MICA_REVIEWER)) {
        builder.setRole(Roles.MICA_REVIEWER);
      } else if (subject.hasRole(Roles.MICA_EDITOR)) {
        builder.setRole(Roles.MICA_EDITOR);
      } else {
        builder.setRole(Roles.MICA_USER);
      }
    } catch(AuthorizationException e) {
      builder.setRole(Roles.MICA_USER);
    }

    return builder.build();
  }

  @GET
  @Path("/username")
  public Response getSubject() {
    // Find the Shiro username
    return Response.ok(SecurityUtils.getSubject().getPrincipal().toString()).build();
  }

}

