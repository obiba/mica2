package org.obiba.mica.web.controller;

import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.interceptor.SessionInterceptor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriUtils;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.Map;
import java.util.NoSuchElementException;

public class BaseController {

  @Inject
  protected MicaConfigService micaConfigService;

  @Inject
  protected SubjectAclService subjectAclService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private VariableSetService variableSetService;

  @ExceptionHandler(NoSuchElementException.class)
  public ModelAndView notFoundError(Exception ex) {
    return makeErrorModelAndView("404", ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ModelAndView unauthorizedError(Exception ex) {
    return makeErrorModelAndView("403", ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ModelAndView forbiddenError(Exception ex) {
    return unauthorizedError(ex);
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView anyError(Exception ex) {
    return makeErrorModelAndView("500", ex.getMessage());
  }

  protected ModelAndView makeErrorModelAndView(String status, String message) {
    ModelAndView mv = new ModelAndView("error");
    mv.getModel().put("status", status);
    mv.getModel().put("msg", message);
    mv.getModel().put("contextPath", micaConfigService.getContextPath());
    mv.getModel().put("config", micaConfigService.getConfig());
    SessionInterceptor.populateUserEntries(mv, userProfileService, variableSetService);
    return mv;
  }

  void checkAccess(String resource, String id) {
    subjectAclService.checkAccess(resource, id);
  }

  boolean isAccessible(String resource, String id) {
    return subjectAclService.isAccessible(resource, id);
  }

  void checkPermission(String resource, String action, String id) {
    subjectAclService.checkPermission(resource, action, id);
  }

  /**
   * Check the permission (action on a resource). If a key is provided and is valid, the permission check is by-passed.
   * If the provided key is not valid, permission check is applied.
   *
   * @param resource
   * @param action
   * @param id
   * @param shareKey
   */
  void checkPermission(String resource, String action, String id, String shareKey) {
    subjectAclService.checkPermission(resource, action, id, shareKey);
  }

  boolean isPermitted(String resource, String action, String id) {
    return subjectAclService.isPermitted(resource, action, id);
  }

  /**
   * Check whether the dataset contingency service is available and if it is (potentially) permitted.
   *
   * @return
   */
  boolean showDatasetContingencyLink() {
    return micaConfigService.getConfig().isContingencyEnabled() &&
      (!subjectAclService.hasDatasetContingencyPermissions() ||
        (!SecurityUtils.getSubject().isAuthenticated() || subjectAclService.isDatasetContingencyPermitted()));
  }

  Map<String, Object> newParameters() {
    return Maps.newHashMap();
  }

  /**
   * Get and clean lang identifier.
   *
   * @param locale   from cookie (dirty string)
   * @param language from query param
   * @return
   */
  protected String getLang(String locale, String language) {
    String lang = language == null ? locale : language;
    return lang == null ? "en" : lang.replaceAll("\"", "");
  }

  protected String encode(String path) {
    try {
      return UriUtils.encode(path, "UTF-8");
    } catch (Exception e) {
      return path;
    }
  }
}
