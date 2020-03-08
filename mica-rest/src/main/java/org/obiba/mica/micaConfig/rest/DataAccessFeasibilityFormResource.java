package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFeasibilityFormService;
import org.obiba.mica.micaConfig.service.DataAccessFeasibilityFormService;
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
@Path("/config/data-access-feasibility-form")
@RequiresAuthentication
public class DataAccessFeasibilityFormResource {

  private DataAccessFormService dataAccessFormService;
  private DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  private Dtos dtos;

  @Inject
  public DataAccessFeasibilityFormResource(DataAccessFeasibilityFormService dataAccessFeasibilityFormService,
                                           DataAccessFormService dataAccessFormService,
                                           Dtos dtos) {
    this.dataAccessFeasibilityFormService = dataAccessFeasibilityFormService;
    this.dataAccessFormService = dataAccessFormService;
    this.dtos = dtos;
  }

  @GET
  public Mica.DataAccessFeasibilityFormDto get() {
    Optional<DataAccessFeasibilityForm> dataAccessFeasibilityForm = dataAccessFeasibilityFormService.find();
    Optional<DataAccessForm> dataAccessForm = dataAccessFormService.find();
    if(!dataAccessForm.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    if (!dataAccessFeasibilityForm.isPresent()) throw NoSuchDataAccessFormException.withMessage("DataAccessFeasibilityForm does not exist");
    return dtos.asDto(dataAccessFeasibilityForm.get(), dataAccessForm.get());
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessFeasibilityFormDto dto) {
    dataAccessFeasibilityFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
