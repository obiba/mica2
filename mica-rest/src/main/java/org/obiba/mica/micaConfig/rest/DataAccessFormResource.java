package org.obiba.mica.micaConfig.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/data-access-form")
public class DataAccessFormResource {

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  Dtos dtos;

  @GET
  @RequiresPermissions("/data-access-requests:ADD")
  public Mica.DataAccessFormDto getDataAccessForm() {
    Optional<DataAccessForm> d = dataAccessFormService.findDataAccessForm();

    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();

    return dtos.asDto(d.get());
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateDataAccessForm(Mica.DataAccessFormDto dto) {
    dataAccessFormService.createOrUpdateDataAccessForm(dtos.fromDto(dto));

    return Response.ok().build();
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publishDataAccessForm() {
    dataAccessFormService.publish();

    return Response.ok().build();
  }
}
