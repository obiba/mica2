package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Path("/config/data-access-form")
public class DataAccessFormResource {

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  DataAccessPermissionsConfigurationService dataAccessPermissionsConfigurationService;

  @Inject
  Dtos dtos;

  @GET
  public Mica.DataAccessFormDto get() {
    Optional<DataAccessForm> d = dataAccessFormService.find();

    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();

    return dtos.asDto(d.get());
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessFormDto dto) {
    dataAccessPermissionsConfigurationService.onSaveDataAccessForm(
      dtos.fromDto(get()).getDataAccessPermissions(),
      dtos.fromDto(dto).getDataAccessPermissions());

    dataAccessFormService.createOrUpdate(dtos.fromDto(dto));

    return Response.ok().build();
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    dataAccessFormService.publish();

    return Response.ok().build();
  }
}
