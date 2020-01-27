package org.obiba.mica.web.controller;

import org.obiba.mica.security.service.SubjectAclService;

import javax.inject.Inject;

public class EntityController {

  @Inject
  private SubjectAclService subjectAclService;

  protected void checkAccess(String resource, String id) {
    subjectAclService.checkAccess(resource, id);
  }

  protected boolean isAccessible(String resource, String id) {
    return subjectAclService.isAccessible(resource, id);
  }

}
