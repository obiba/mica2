package org.obiba.mica.micaConfig.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchProjectFormException;
import org.obiba.mica.micaConfig.domain.ProjectForm;
import org.obiba.mica.micaConfig.service.ProjectFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.rest.SubjectAclResource;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Path("/config/project-form")
public class ProjectFormResource {

  @Inject
  ProjectFormService projectFormService;

  @Inject
  ApplicationContext applicationContext;

  @Inject
  Dtos dtos;

  @GET
  public Mica.ProjectFormDto get() {
    Optional<ProjectForm> d = projectFormService.find();

    if(!d.isPresent()) throw NoSuchProjectFormException.withDefaultMessage();

    return dtos.asDto(d.get());
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.ProjectFormDto dto) {
    projectFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    projectFormService.publish();
    return Response.ok().build();
  }

  @Path("/permissions")
  @RequiresRoles(Roles.MICA_ADMIN)
  public SubjectAclResource permissions(@QueryParam("draft") @DefaultValue("false") boolean draft) {
    SubjectAclResource subjectAclResource = applicationContext.getBean(SubjectAclResource.class);
    if (draft) {
      subjectAclResource.setResourceInstance("/draft/project", "*");
      subjectAclResource.setFileResourceInstance("/file", "/draft/project");
    } else {
      subjectAclResource.setResourceInstance("/project", "*");
      subjectAclResource.setFileResourceInstance("/file", "/project");
    }
    return subjectAclResource;
  }
}
