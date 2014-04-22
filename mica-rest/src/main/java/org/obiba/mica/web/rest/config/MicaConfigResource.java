package org.obiba.mica.web.rest.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.obiba.mica.service.MicaConfigService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.codahale.metrics.annotation.Timed;

@Path("/config")
public class MicaConfigResource {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private Dtos dtos;

  @GET
  @Timed
  public Mica.MicaConfigDto get() {
    return dtos.asDto(micaConfigService.getConfig());
  }

  @PUT
  @Timed
  public Response create(@SuppressWarnings("TypeMayBeWeakened") Mica.MicaConfigDto dto) {
    micaConfigService.save(dtos.fromDto(dto));
    return Response.noContent().build();
  }
}
