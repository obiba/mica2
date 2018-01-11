/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    subjectAclResource.setFileResourceInstance("/draft/file", String.format("/%s", getTarget()));
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
