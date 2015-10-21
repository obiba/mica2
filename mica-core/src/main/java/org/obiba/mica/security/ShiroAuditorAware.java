package org.obiba.mica.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class ShiroAuditorAware implements AuditorAware<String> {

  @Override
  public String getCurrentAuditor() {
    Subject subject = SecurityUtils.getSubject();
    return subject == null || subject.getPrincipal() == null ? "Anonymous" : subject.getPrincipal().toString();
  }

}
