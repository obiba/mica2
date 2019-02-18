package org.obiba.mica.micaConfig.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

@Component
@Path("/config/data-access-amendment-form")
public class DataAccessAmendmentFormResource {

  private DataAccessFormService dataAccessFormService;
  private DataAccessAmendmentFormService dataAccessAmendmentFormService;

  private Dtos dtos;

  @Inject
  public DataAccessAmendmentFormResource(DataAccessAmendmentFormService dataAccessAmendmentFormService,
                                         DataAccessFormService dataAccessFormService,
                                         Dtos dtos) {
    this.dataAccessAmendmentFormService = dataAccessAmendmentFormService;
    this.dataAccessFormService = dataAccessFormService;
    this.dtos = dtos;
  }

  @GET
  public Mica.DataAccessAmendmentFormDto get() {
    Optional<DataAccessAmendmentForm> dataAccessAmendmentForm = dataAccessAmendmentFormService.find();
    Optional<DataAccessForm> dataAccessForm = dataAccessFormService.find();
    if(!dataAccessForm.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    if (!dataAccessAmendmentForm.isPresent()) throw NoSuchDataAccessFormException.withMessage("DataAccessAmendmentForm does not exist");
    return dtos.asDto(dataAccessAmendmentForm.get(), dataAccessForm.get());
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessAmendmentFormDto dto) {
    dataAccessAmendmentFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
