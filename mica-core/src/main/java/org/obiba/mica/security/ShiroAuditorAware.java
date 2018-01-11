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
