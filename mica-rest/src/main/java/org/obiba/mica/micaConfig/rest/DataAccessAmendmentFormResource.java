package org.obiba.mica.micaConfig.rest;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.micaConfig.service.DataAccessConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
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
  public Mica.DataAccessAmendmentFormDto get(@QueryParam("revision") String revision) {
    Optional<DataAccessAmendmentForm> d = dataAccessAmendmentFormService.findByRevision(revision);
    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    return dtos.asDto(d.get(), dataAccessConfigService.getOrCreateConfig());
  }

  @PUT
  @Path("/_publish")
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response publish() {
    dataAccessAmendmentFormService.publish();
    return Response.ok().build();
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(Mica.DataAccessAmendmentFormDto dto) {
    dataAccessAmendmentFormService.createOrUpdate(dtos.fromDto(dto));
    return Response.ok().build();
  }

}
