/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security;

import java.util.concurrent.Callable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.shiro.authc.SudoAuthToken;

public class SubjectUtils {

  public static final String ANONYMOUS_USER_KEY = "_uid";

  private SubjectUtils() {
  }

  public static <V> V sudo(Callable<V> callable) {
    Subject sudo = new Subject.Builder().principals(
      SecurityUtils.getSecurityManager().authenticate(new SudoAuthToken(SecurityUtils.getSubject())).getPrincipals())
      .authenticated(true).buildSubject();

    return sudo.execute(callable);
  }

  public static String getAnonymousUserId(HttpServletRequest request) {
    // get from attributes
    Object uid = request.getAttribute(ANONYMOUS_USER_KEY);
    if (uid == null) {
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie ck : cookies) {
          if (ck.getName().equals(ANONYMOUS_USER_KEY)) {
            uid = ck.getValue();
          }
        }
      }
    }
    return uid == null ? null : uid.toString();
  }
}
