package org.obiba.mica.core.security.realm;

import java.util.Collection;

import javax.inject.Inject;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.util.PermissionUtils;
import org.obiba.mica.core.security.Roles;
import org.springframework.stereotype.Component;

/**
 * Turns a role defined by any realm, into permissions in mica.
 */
@Component
public class MicaRolePermissionResolver implements RolePermissionResolver {

  @Inject
  private MicaPermissionResolver permissionResolver;

  @Override
  public Collection<Permission> resolvePermissionsInRole(String roleString) {
    switch(roleString) {
      case Roles.MICA_ADMIN:
        return PermissionUtils.resolveDelimitedPermissions("*", permissionResolver);
      case Roles.MICA_REVIEWER:
        return PermissionUtils
          .resolveDelimitedPermissions("mica:/draft:EDIT,mica:/draft:PUBLISH,mica:/files:UPLOAD", permissionResolver);
      case Roles.MICA_EDITOR:
        return PermissionUtils.resolveDelimitedPermissions("mica:/draft:EDIT,mica:/files:UPLOAD", permissionResolver);
      case Roles.MICA_USER:
        return null;
    }
    return null;
  }
}
