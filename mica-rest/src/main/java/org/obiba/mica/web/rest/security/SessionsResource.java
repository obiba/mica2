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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.config.JerseyConfiguration;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.service.DocumentSetService;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkSetService;
import org.obiba.mica.security.SubjectUtils;
import org.obiba.mica.study.service.StudySetService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.NoSuchOtpException;
import org.obiba.shiro.authc.UsernamePasswordOtpToken;
import org.obiba.shiro.realm.ObibaRealm;
import org.obiba.shiro.web.filter.AuthenticationExecutor;
import org.obiba.shiro.web.filter.UserBannedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

  @Inject
  private VariableSetService variableSetService;

  @Inject
  private StudySetService studySetService;

  @Inject
  private NetworkSetService networkSetService;

  @POST
  @Path("/sessions")
  public Response createSession(@SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest request,
                                @FormParam("username") String username, @FormParam("password") String password) {
    try {
      ObibaRealm.Subject profile = userProfileService.getProfile(username);
      String realUsername = profile == null ? username : profile.getUsername();
      authenticationExecutor.login(makeUsernamePasswordToken(realUsername, password, request));

      Subject subject = SecurityUtils.getSubject();
      String sessionId = subject.getSession().getId().toString();
      log.info("Successful session creation for user '{}' session ID is '{}'.", realUsername, sessionId);
      String locale = getPreferredLocale(subject);

      mergeAnonymousUserCarts(request);

      Response.ResponseBuilder builder = Response
        .created(UriBuilder.fromPath(JerseyConfiguration.WS_ROOT).path(SessionResource.class).build(sessionId));

      if (!Strings.isNullOrEmpty(locale))
        builder.cookie(new NewCookie("NG_TRANSLATE_LANG_KEY", locale, micaConfigService.getContextPath() + "/", null, DEFAULT_VERSION, null, DEFAULT_MAX_AGE, null, false, false));

      return builder.build();
    } catch (UserBannedException e) {
      throw e;
    } catch (NoSuchOtpException e) {
      return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", e.getOtpHeader()).build();
    } catch (AuthenticationException e) {
      log.info("Authentication failure of user '{}' at ip: '{}': {}", username, request.getRemoteAddr(),
        e.getMessage());
      // When a request contains credentials and they are invalid, the 403 (Forbidden) should be returned.
      return Response.status(Response.Status.FORBIDDEN).cookie().build();
    }
  }

  /**
   * On login, anonymous cart is merged into the subject's one,
   * to ensure a continuity with the catalog navigation.
   *
   * @param request
   */
  private void mergeAnonymousUserCarts(HttpServletRequest request) {
    MicaConfig micaConfig = micaConfigService.getConfig();
    if (!micaConfig.isCartEnabled() || !micaConfig.isAnonymousCanCreateCart())
      return;

    String uid = SubjectUtils.getAnonymousUserId(request);
    if (!Strings.isNullOrEmpty(uid)) {
      if (micaConfig.isStudyDatasetEnabled() || micaConfig.isHarmonizationDatasetEnabled())
        mergeCart(uid, variableSetService);
      if (!micaConfig.isSingleStudyEnabled() && micaConfig.isStudiesCartEnabled())
        mergeCart(uid, studySetService);
      if (micaConfig.isNetworkEnabled() && !micaConfig.isSingleNetworkEnabled() && micaConfig.isNetworksCartEnabled())
        mergeCart(uid, networkSetService);
    }
  }

  private void mergeCart(String uid, DocumentSetService documentSetService) {
    DocumentSet anonymousCart = documentSetService.getCartAnonymousUser(uid);
    if (!anonymousCart.getIdentifiers().isEmpty()) {
      try {
        // merge into subject's cart
        DocumentSet subjectCart = documentSetService.getCartCurrentUser();
        documentSetService.addIdentifiers(subjectCart.getId(), Lists.newArrayList(anonymousCart.getIdentifiers()));
        // FIXME delete or empty the anonymous set breaks the merge (?)
      } catch (Exception e) {
        // ignore
      }
    }
  }

  private String getPreferredLocale(Subject subject) {
    ObibaRealm.Subject profile = userProfileService.getProfile(subject.getPrincipal().toString());
    List<Map<String, String>> attrs = profile.getAttributes();
    if (attrs != null) {
      Optional<String> locale = attrs.stream().filter(attr -> attr.getOrDefault("key", "").equals("locale"))
        .map(attr -> attr.getOrDefault("value", ""))
        .findFirst();
      return locale.orElse("");
    }
    return "";
  }

  private UsernamePasswordToken makeUsernamePasswordToken(String username, String password, HttpServletRequest
    request) {
    String otp = request.getHeader("X-Obiba-TOTP");
    if (!Strings.isNullOrEmpty(otp))
      return new UsernamePasswordOtpToken(username, password, otp);
    return new UsernamePasswordToken(username, password);
  }
}

