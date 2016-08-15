package org.obiba.mica.micaConfig.rest;

import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.springframework.context.ApplicationContext;

public interface PermissionAwareResource {

  @Path("/permissions")
  @RequiresRoles(Roles.MICA_ADMIN)
  default SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = getApplicationContext().getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance(String.format("/draft/%s", getTarget()), "*");
    subjectAclResource.setFileResourceInstance("/file", String.format("/draft/%s", getTarget()));
    return subjectAclResource;
  }

  @Path("/accesses")
  @RequiresRoles(Roles.MICA_ADMIN)
  default SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = getApplicationContext().getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance(String.format("/%s", getTarget()), "*");
    subjectAclResource.setFileResourceInstance("/file", String.format("/%s", getTarget()));
    return subjectAclResource;
  }

  String getTarget();

  ApplicationContext getApplicationContext();
}
