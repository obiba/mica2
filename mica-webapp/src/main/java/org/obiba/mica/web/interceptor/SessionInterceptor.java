package org.obiba.mica.web.interceptor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.security.Roles;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;
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
import java.util.concurrent.TimeUnit;

@Component
public class SessionInterceptor extends HandlerInterceptorAdapter {

  private static final Logger log = LoggerFactory.getLogger(SessionInterceptor.class);

  private final UserProfileService userProfileService;

  private Cache<String, ObibaRealm.Subject> subjectCache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .build();

  @Inject
  public SessionInterceptor(UserProfileService userProfileService) {
    this.userProfileService = userProfileService;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      String username = subject.getPrincipal().toString();
      ObibaRealm.Subject profile = subjectCache.getIfPresent(username);
      try {
        if (profile == null) {
          profile = userProfileService.getProfile(username);
          subjectCache.put(username, profile);
        }
        List<String> roles = Lists.newArrayList(Roles.MICA_ADMIN, Roles.MICA_REVIEWER, Roles.MICA_EDITOR, Roles.MICA_DAO, Roles.MICA_USER);
        boolean[] result = subject.hasRoles(roles);
        for (int i = result.length - 1; i >= 0; i--) {
          if (!result[i]) roles.remove(i);
        }
        Map<String, Object> params = Maps.newHashMap();
        String fullName = username;
        Map<String, String> attributes = Maps.newHashMap();
        if (profile.getAttributes() != null) {
          profile.getAttributes().forEach(attr -> attributes.put(attr.get("key"), attr.get("value")));
          fullName = attributes.getOrDefault("firstName", "") + (attributes.containsKey("firstName") ? " " : "") + attributes.getOrDefault("lastName", username);
        }
        params.put("username", username);
        params.put("fullName", fullName);
        params.put("attributes", attributes);
        params.put("roles", roles);
        modelAndView.getModel().put("user", params);
      } catch (Exception e) {
        log.warn("Cannot retrieve profile of user {}", username, e);
      }
    }
  }

}
