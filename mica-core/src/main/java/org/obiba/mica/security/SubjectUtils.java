package org.obiba.mica.security;

import java.util.concurrent.Callable;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.shiro.authc.SudoAuthToken;

public class SubjectUtils {
  private SubjectUtils() {
  }

  public static <V> V sudo(Callable<V> callable) {
    Subject sudo = new Subject.Builder().principals(
      SecurityUtils.getSecurityManager().authenticate(new SudoAuthToken(SecurityUtils.getSubject())).getPrincipals())
      .authenticated(true).buildSubject();

    return sudo.execute(callable);
  }
}
