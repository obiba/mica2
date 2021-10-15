package org.obiba.mica.web.interceptor;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.network.service.NetworkSetService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.study.service.StudySetService;
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
import java.util.stream.Collectors;

@Component
public class SessionInterceptor extends HandlerInterceptorAdapter {

  private static final Logger log = LoggerFactory.getLogger(SessionInterceptor.class);

  private final UserProfileService userProfileService;

  private final VariableSetService variableSetService;

  private final StudySetService studySetService;

  private final NetworkSetService networkSetService;

  @Inject
  public SessionInterceptor(UserProfileService userProfileService, VariableSetService variableSetService, StudySetService studySetService, NetworkSetService networkSetService) {
    this.userProfileService = userProfileService;
    this.variableSetService = variableSetService;
    this.studySetService = studySetService;
    this.networkSetService = networkSetService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    populateUserEntries(modelAndView, userProfileService, variableSetService, studySetService, networkSetService);
  }

  public static void populateUserEntries(ModelAndView modelAndView, UserProfileService userProfileService,
                                         VariableSetService variableSetService, StudySetService studySetService, NetworkSetService networkSetService) {
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
        params.put("variablesLists", variableSetService.getAllCurrentUser().stream().filter(DocumentSet::hasName).collect(Collectors.toList()));
        params.put("studiesCart", new Cart(studySetService.getCartCurrentUser()));
        params.put("networksCart", new Cart(networkSetService.getCartCurrentUser()));
        modelAndView.getModel().put("user", params);
      } catch (Exception e) {
        log.warn("Cannot retrieve profile of user {}", username, e);
      }
    }
  }

}
