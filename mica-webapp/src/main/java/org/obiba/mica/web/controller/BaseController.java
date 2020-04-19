package org.obiba.mica.web.controller;

import com.google.common.collect.Maps;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.NoSuchElementException;

public class BaseController {

  @Inject
  private SubjectAclService subjectAclService;

  @ExceptionHandler(NoSuchElementException.class)
  public ModelAndView notFoundError(Exception ex) {
    return makeErrorModelAndView("404", ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView anyError(Exception ex) {
    return makeErrorModelAndView("500", ex.getMessage());
  }

  protected ModelAndView makeErrorModelAndView(String status, String message) {
    ModelAndView mv = new ModelAndView("error");
    mv.getModel().put("status", status);
    mv.getModel().put("msg", message);
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

  Map<String, Object> newParameters() {
    return Maps.newHashMap();
  }

  /**
   * Get and clean lang identifier.
   *
   * @param locale from cookie (dirty string)
   * @param language from query param
   * @return
   */
  protected String getLang(String locale, String language) {
    String lang = language == null ? locale : language;
    return lang == null ? "en" : lang.replaceAll("\"", "");
  }
}
