/*******************************************************************************
 * Copyright 2014(c) The OBiBa Consortium. All rights reserved.
 * <p>
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.mica.web.rest.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Mica;
import org.obiba.shiro.realm.ObibaRealm;
import org.springframework.stereotype.Component;

@Component
@Path("/auth/session/_current")
@RequiresAuthentication
public class CurrentSessionResource {

  @DELETE
  public Response deleteSession(@Context HttpServletRequest request) {
    // Delete the Shiro session
    try {
      Session session = SecurityUtils.getSubject().getSession();
      Object cookieValue = session.getAttribute(HttpHeaders.SET_COOKIE);
      SecurityUtils.getSubject().logout();

      if(cookieValue != null) {
        NewCookie cookie = NewCookie.valueOf(cookieValue.toString());
        if(ObibaRealm.TICKET_COOKIE_NAME.equals(cookie.getName())) {
          return Response.ok().header(HttpHeaders.SET_COOKIE,
              new NewCookie(ObibaRealm.TICKET_COOKIE_NAME, null, "/", cookie.getDomain(), "Obiba session deleted", 0,
                  cookie.isSecure())).build();
        }
      }
      return Response.ok().build();
    } catch(SessionException e) {
      return getForbiddenResponse(request);
    }
  }

  @GET
  public Response get(@Context HttpServletRequest request) {
    try {
      Subject subject = SecurityUtils.getSubject();
      Mica.SessionDto.Builder builder = Mica.SessionDto.newBuilder() //
          .setUsername(subject.getPrincipal().toString());
      List<String> roles = //
          Arrays.asList(Roles.MICA_ADMIN, Roles.MICA_REVIEWER, Roles.MICA_EDITOR, Roles.MICA_DAO, Roles.MICA_USER); //

      boolean[] result = subject.hasRoles(roles);
      IntStream.range(0, result.length).filter(i -> result[i]).forEach(i -> builder.addRoles(roles.get(i)));
      return Response.ok(builder.build()).build();
    } catch(SessionException e) {
      return getForbiddenResponse(request);
    }
  }

  @GET
  @Path("/username")
  public Response getSubject(@Context HttpServletRequest request) {
    try {
      return Response.ok(SecurityUtils.getSubject().getPrincipal().toString()).build();
    } catch(SessionException e) {
      return getForbiddenResponse(request);
    }
  }

  private Response getForbiddenResponse(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if(cookies != null) {
      Optional<Cookie> obibaCookie = Stream.of(cookies).filter(c -> c.getName().equals(ObibaRealm.TICKET_COOKIE_NAME))
          .findFirst();
      if(obibaCookie.isPresent()) {
        return Response.status(Response.Status.FORBIDDEN).header(HttpHeaders.SET_COOKIE,
            new NewCookie(ObibaRealm.TICKET_COOKIE_NAME, null, "/", obibaCookie.get().getDomain(), "Obiba session deleted", 0,
                obibaCookie.get().getSecure())).build();
      }
    }
    return Response.status(Response.Status.FORBIDDEN).build();
  }

}

