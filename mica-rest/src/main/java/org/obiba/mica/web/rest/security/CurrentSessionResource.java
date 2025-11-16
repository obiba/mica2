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

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/auth/session/_current")
@RequiresAuthentication
public class CurrentSessionResource {

  private static final String OBIBA_ID_COOKIE_NAME = "obibaid";

  @Inject
  private MicaConfigService micaConfigService;

  @DELETE
  public Response deleteSession() {
    // Delete the Shiro session
    try {
      Session session = SecurityUtils.getSubject().getSession();
      Object cookieValue = session.getAttribute(HttpHeaders.SET_COOKIE);
      SecurityUtils.getSubject().logout();

      if(cookieValue != null) {
        NewCookie cookie = NewCookie.valueOf(cookieValue.toString());
        if (OBIBA_ID_COOKIE_NAME.equals(cookie.getName())) {
          return Response.ok().header(HttpHeaders.SET_COOKIE,
            new NewCookie.Builder(OBIBA_ID_COOKIE_NAME)
              .value(null)
              .path(micaConfigService.getContextPath() + "/")
              .domain(cookie.getDomain())
              .comment("Obiba session deleted")
              .maxAge(0)
              .httpOnly(true)
              .secure(true)
              .sameSite(NewCookie.SameSite.LAX)
              .build()).build();
        }
      }
    } catch(InvalidSessionException e) {
      // Ignore
    }
    return Response.ok().build();
  }

  @GET
  public Mica.SessionDto get() {
    Subject subject = SecurityUtils.getSubject();
    Mica.SessionDto.Builder builder = Mica.SessionDto.newBuilder() //
      .setUsername(subject.getPrincipal().toString());
    List<String> roles = //
      Arrays.asList(Roles.MICA_ADMIN, Roles.MICA_REVIEWER, Roles.MICA_EDITOR, Roles.MICA_DAO, Roles.MICA_USER); //

    boolean[] result = subject.hasRoles(roles);
    IntStream.range(0, result.length).filter(i -> result[i]).forEach(i -> builder.addRoles(roles.get(i)));
    return builder.build();
  }

  @GET
  @Path("/username")
  public Response getSubject() {
    // Find the Shiro username
    return Response.ok(SecurityUtils.getSubject().getPrincipal().toString()).build();
  }

}

