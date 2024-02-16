package org.obiba.mica.web.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.service.NetworkSetService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.SubjectUtils;
import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.service.StudySetService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.Cart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Component
public class SessionInterceptor implements AsyncHandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(SessionInterceptor.class);

  private final MicaConfigService micaConfigService;

  private final UserProfileService userProfileService;

  private final VariableSetService variableSetService;

  private final StudySetService studySetService;

  private final NetworkSetService networkSetService;

  private final SubjectAclService subjectAclService;

  private static final String[] ALL_DRAFT_RESOURCES = {
    "/draft/network",
    "/draft/individual-study",
    "/draft/harmonization-study",
    "/draft/collected-dataset",
    "/draft/harmonized-dataset",
    "/draft/project"
  };

  @Inject
  public SessionInterceptor(MicaConfigService micaConfigService, UserProfileService userProfileService, VariableSetService variableSetService,
    StudySetService studySetService, NetworkSetService networkSetService, SubjectAclService subjectAclService) {
    this.micaConfigService = micaConfigService;
    this.userProfileService = userProfileService;
    this.variableSetService = variableSetService;
    this.studySetService = studySetService;
    this.networkSetService = networkSetService;
    this.subjectAclService = subjectAclService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    if (modelAndView == null) return;
    populateUserEntries(request, modelAndView, micaConfigService, userProfileService, variableSetService, studySetService, networkSetService, subjectAclService);
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Subject subject = SecurityUtils.getSubject();
    if (!subject.isAuthenticated()) {
      String uid = SubjectUtils.getAnonymousUserId(request);
      if (Strings.isNullOrEmpty(uid)) {
        String userId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(SubjectUtils.ANONYMOUS_USER_KEY, userId);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
        request.setAttribute(SubjectUtils.ANONYMOUS_USER_KEY, userId);
      }
    }
    return true;
  }

  public static void populateUserEntries(HttpServletRequest request, ModelAndView modelAndView, MicaConfigService micaConfigService, UserProfileService userProfileService,
                                         VariableSetService variableSetService, StudySetService studySetService, NetworkSetService networkSetService, SubjectAclService subjectAclService) {
    boolean isRedirect =  !Strings.isNullOrEmpty(modelAndView.getViewName()) && modelAndView.getViewName().startsWith("redirect:");
    if (isRedirect) return;
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      String username = subject.getPrincipal().toString();
      try {
        Map<String, Object> params = userProfileService.getProfileMap(username, true);
        List<String> roles = Lists.newArrayList(Roles.MICA_ADMIN, Roles.MICA_REVIEWER, Roles.MICA_EDITOR, Roles.MICA_DAO, Roles.MICA_USER);
        boolean[] result = subject.hasRoles(roles);
        for (int i = result.length - 1; i >= 0; i--) {
          if (!result[i]) roles.remove(i);
        }

        if (params.containsKey("attributes")) {
          try {
            Map<String, String> attrs = (Map<String, String>) params.get("attributes");
            if (attrs.containsKey("otpEnabled")) {
              params.put("otpEnabled", Boolean.parseBoolean(attrs.get("otpEnabled")));
              attrs.remove("otpEnabled");
            }
          } catch (Exception e) {
            // ignore (probably cast error)
          }
        }

        params.put("realm", subject.getPrincipals().getRealmNames().iterator().next());
        params.put("roles", roles);
        params.put("hasPermissionOnAnyDraftDocument", subjectAclService.findBySubject(subject.getPrincipal().toString(), SubjectAcl.Type.USER).stream().anyMatch(acl -> Arrays.stream(ALL_DRAFT_RESOURCES).anyMatch(res -> res.equals(acl.getResource()))));
        modelAndView.getModel().put("user", params);

        params = Maps.newHashMap();
        params.put("variablesCart", new Cart(variableSetService.getCartCurrentUser()));
        params.put("variablesLists", variableSetService.getAllCurrentUser().stream().filter(DocumentSet::hasName).collect(Collectors.toList()));
        params.put("studiesCart", new Cart(studySetService.getCartCurrentUser()));
        params.put("networksCart", new Cart(networkSetService.getCartCurrentUser()));
        modelAndView.getModel().put("sets", params);

      } catch (Exception e) {
        log.warn("Cannot retrieve profile of user {}", username, e);
      }
    } else if (micaConfigService.getConfig().isAnonymousCanCreateCart()) {
      Map<String, Object> params = Maps.newHashMap();
      Object uid = SubjectUtils.getAnonymousUserId(request);
      String userId = uid != null ? uid.toString() : "anonymous";
      params.put("variablesCart", new Cart(variableSetService.getCartAnonymousUser(userId)));
      params.put("studiesCart", new Cart(studySetService.getCartAnonymousUser(userId)));
      params.put("networksCart", new Cart(networkSetService.getCartAnonymousUser(userId)));

      modelAndView.getModel().put("sets", params);
    }
  }

}
