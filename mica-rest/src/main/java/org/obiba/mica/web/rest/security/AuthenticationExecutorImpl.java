/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.security;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.shiro.subject.Subject;
import org.obiba.mica.file.service.FileSystemService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.shiro.web.filter.AbstractAuthenticationExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationExecutorImpl extends AbstractAuthenticationExecutor, InitializingBean {

  @Inject
  private FileSystemService fileSystemService;

  @Inject
  private SubjectAclService subjectAclService;

  @Value("${login.maxTry:3}")
  private int maxTry;

  @Value("${login.trialTime:300}")
  private int trialTime;

  @Value("${login.banTime:300}")
  private int banTime;


  @Override
  public void afterPropertiesSet() throws Exception {
    configureBan(maxTry, trialTime, banTime);
  }

  @Override
  protected void ensureProfile(Subject subject) {
    String name = subject.getPrincipal().toString();
    fileSystemService.mkdirs("/user/" + name);
    subjectAclService.addUserPermission(name, "/draft/file", "ADD,VIEW,EDIT,DELETE", String.format("/user/%s", name));
  }
}
