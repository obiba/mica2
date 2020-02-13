package org.obiba.mica.web.controller;

import com.google.common.collect.Maps;
import org.obiba.mica.security.service.SubjectAclService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class BaseController {

  @Inject
  private SubjectAclService subjectAclService;

  void checkAccess(String resource, String id) {
    subjectAclService.checkAccess(resource, id);
  }

  boolean isAccessible(String resource, String id) {
    return subjectAclService.isAccessible(resource, id);
  }

  void checkPermission(String resource, String action, String id) {
    subjectAclService.checkPermission(resource, action, id);
  }

  boolean isPermitted(String resource, String action, String id) {
    return subjectAclService.isPermitted(resource, action, id);
  }

  Map<String, Object> newParameters() {
    return Maps.newHashMap();
  }
}
