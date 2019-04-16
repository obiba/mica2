package org.obiba.mica.security;

import javax.inject.Inject;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.springframework.stereotype.Component;

@Component
public class MicaWebEnvironment extends DefaultWebEnvironment {

  @Inject
  public MicaWebEnvironment(SessionsSecurityManager securityManager) {
    super();
    this.setSecurityManager(securityManager);
  }

}
