package org.obiba.mica.micaConfig.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchProjectFormException;
import org.obiba.mica.micaConfig.domain.ProjectForm;
import org.obiba.mica.micaConfig.service.ProjectFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/project-form")
public class ProjectFormResource {

  @Inject
  ProjectFormService projectFormService;

  @Inject
  Dtos dtos;

  @GET
  @RequiresPermissions("/project-request:ADD")
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
}
