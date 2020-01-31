package org.obiba.mica.web.controller;

import org.obiba.mica.security.service.SubjectAclService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

public class EntityController {

  @Inject
  private SubjectAclService subjectAclService;

  protected void checkAccess(String resource, String id) {
    subjectAclService.checkAccess(resource, id);
  }

  protected boolean isAccessible(String resource, String id) {
    return subjectAclService.isAccessible(resource, id);
  }

  protected boolean isPermitted(String resource, String action, String id) {
    return subjectAclService.isPermitted(resource, action, id);
  }
}
