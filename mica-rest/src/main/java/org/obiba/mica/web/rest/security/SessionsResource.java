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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.config.JerseyConfiguration;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;
import org.obiba.shiro.web.filter.AuthenticationExecutor;
import org.obiba.shiro.web.filter.UserBannedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.core.Cookie.DEFAULT_VERSION;
import static javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Component
@Path("/auth")
public class SessionsResource {

  private static final Logger log = LoggerFactory.getLogger(SessionsResource.class);

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private AuthenticationExecutor authenticationExecutor;

  @Inject
  private UserProfileService userProfileService;

  @POST
  @Path("/sessions")
  public Response createSession(@SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest servletRequest,
      @FormParam("username") String username, @FormParam("password") String password) {
    try {
      ObibaRealm.Subject profile = userProfileService.getProfile(username);
      String realUsername = profile == null ? username : profile.getUsername();
      authenticationExecutor.login(new UsernamePasswordToken(realUsername, password));

      Subject subject = SecurityUtils.getSubject();
      String sessionId = subject.getSession().getId().toString();
      log.info("Successful session creation for user '{}' session ID is '{}'.", realUsername, sessionId);
      String locale = getPreferredLocale(subject);

      Response.ResponseBuilder builder = Response
        .created(UriBuilder.fromPath(JerseyConfiguration.WS_ROOT).path(SessionResource.class).build(sessionId));

      if (!Strings.isNullOrEmpty(locale))
        builder.cookie(new NewCookie("NG_TRANSLATE_LANG_KEY", locale, micaConfigService.getContextPath() + "/", null, DEFAULT_VERSION, null, DEFAULT_MAX_AGE, null, false, false));

      return builder.build();
    } catch(UserBannedException e) {
      throw e;
    } catch(AuthenticationException e) {
      log.info("Authentication failure of user '{}' at ip: '{}': {}", username, servletRequest.getRemoteAddr(),
          e.getMessage());
      // When a request contains credentials and they are invalid, the 403 (Forbidden) should be returned.
      return Response.status(Response.Status.FORBIDDEN).cookie().build();
    }
  }

  private String getPreferredLocale(Subject subject) {
    ObibaRealm.Subject profile = userProfileService.getProfile(subject.getPrincipal().toString());
    List<Map<String, String>> attrs = profile.getAttributes();
    if (attrs != null) {
      Optional<String> locale = attrs.stream().filter(attr -> attr.getOrDefault("key","").equals("locale"))
        .map(attr -> attr.getOrDefault("value", ""))
        .findFirst();
      return locale.orElse("");
    }
    return "";
  }
}

