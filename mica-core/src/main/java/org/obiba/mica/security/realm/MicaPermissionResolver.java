package org.obiba.mica.security.realm;

import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.springframework.stereotype.Component;

@Component
public class MicaPermissionResolver extends WildcardPermissionResolver {

  public MicaPermissionResolver() {

  }

}

