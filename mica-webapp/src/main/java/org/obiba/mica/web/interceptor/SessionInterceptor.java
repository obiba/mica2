package org.obiba.mica.web.interceptor;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.Cart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component
public class SessionInterceptor extends HandlerInterceptorAdapter {

  private static final Logger log = LoggerFactory.getLogger(SessionInterceptor.class);

  private final UserProfileService userProfileService;

  private final VariableSetService variableSetService;

  @Inject
  public SessionInterceptor(UserProfileService userProfileService, VariableSetService variableSetService) {
    this.userProfileService = userProfileService;
    this.variableSetService = variableSetService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    populateUserEntries(modelAndView, userProfileService, variableSetService);
  }

  public static void populateUserEntries(ModelAndView modelAndView, UserProfileService userProfileService, VariableSetService variableSetService) {
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
        params.put("roles", roles);
        params.put("variablesCart", new Cart(variableSetService.getCartCurrentUser()));
        modelAndView.getModel().put("user", params);
      } catch (Exception e) {
        log.warn("Cannot retrieve profile of user {}", username, e);
      }
    }
  }

}
