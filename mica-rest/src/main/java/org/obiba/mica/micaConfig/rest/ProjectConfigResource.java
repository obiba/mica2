/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchProjectFormException;
import org.obiba.mica.micaConfig.domain.ProjectConfig;
import org.obiba.mica.micaConfig.service.ProjectConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Path("/config/project")
public class ProjectConfigResource {

  @Inject
  ProjectConfigService projectConfigService;

  @Inject
  ApplicationContext applicationContext;

  @Inject
  Dtos dtos;

  @GET
  @Path("/form")
  public Mica.ProjectFormDto get() {
    Optional<ProjectConfig> d = projectConfigService.find();
    if(!d.isPresent()) throw NoSuchProjectFormException.withDefaultMessage();
    return dtos.asDto(d.get());
  }

  @PUT
  @Path("/form")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.ProjectFormDto dto) {
    projectConfigService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

  @Path("/permissions")
  @RequiresRoles(Roles.MICA_ADMIN)
  public SubjectAclResource permissions() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/draft/project", "*");
    subjectAclResource.setFileResourceInstance("/file", "/draft/project");
    return subjectAclResource;
  }

  @Path("/accesses")
  @RequiresRoles(Roles.MICA_ADMIN)
  public SubjectAclResource accesses() {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    subjectAclResource.setResourceInstance("/project", "*");
    subjectAclResource.setFileResourceInstance("/file", "/project");
    return subjectAclResource;
  }

}
