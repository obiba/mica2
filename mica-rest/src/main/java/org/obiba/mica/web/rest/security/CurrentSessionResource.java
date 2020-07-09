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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

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
            new NewCookie(OBIBA_ID_COOKIE_NAME, null, micaConfigService.getContextPath() + "/", cookie.getDomain(), "Obiba session deleted", 0, cookie.isSecure())).build();
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

