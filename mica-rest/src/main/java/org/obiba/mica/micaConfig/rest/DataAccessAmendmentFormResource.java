package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
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
@Path("/config/data-access-amendment-form")
@RequiresAuthentication
public class DataAccessAmendmentFormResource {

  private DataAccessConfigService dataAccessConfigService;
  private DataAccessAmendmentFormService dataAccessAmendmentFormService;

  private Dtos dtos;

  @Inject
  public DataAccessAmendmentFormResource(DataAccessAmendmentFormService dataAccessAmendmentFormService,
                                         DataAccessConfigService dataAccessConfigService,
                                         Dtos dtos) {
    this.dataAccessAmendmentFormService = dataAccessAmendmentFormService;
    this.dataAccessConfigService = dataAccessConfigService;
    this.dtos = dtos;
  }

  @GET
  public Mica.DataAccessAmendmentFormDto get() {
    Optional<DataAccessAmendmentForm> dataAccessAmendmentForm = dataAccessAmendmentFormService.find();
    DataAccessConfig dataAccessConfig = dataAccessConfigService.getOrCreateConfig();
    if (!dataAccessAmendmentForm.isPresent())
      throw NoSuchDataAccessFormException.withMessage("DataAccessAmendmentForm does not exist");
    return dtos.asDto(dataAccessAmendmentForm.get(), dataAccessConfig);
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessAmendmentFormDto dto) {
    dataAccessAmendmentFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
