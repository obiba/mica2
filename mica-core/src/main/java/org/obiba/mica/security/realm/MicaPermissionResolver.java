package org.obiba.mica.security.realm;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.springframework.stereotype.Component;

@Component
public class MicaPermissionResolver implements PermissionResolver {

  @Override
  public Permission resolvePermission(String permissionString) {
    return new ExtendedWildcardPermission(permissionString);
  }

}

