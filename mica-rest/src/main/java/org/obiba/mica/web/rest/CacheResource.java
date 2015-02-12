package org.obiba.mica.web.rest;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.micaConfig.service.OpalService;

@Path("/cache")
public class CacheResource {

  @Inject
  private CacheService cacheService;

  @POST
  @Path("/clear")
  public Response clearCaches() {
    cacheService.clearCache();
    return Response.ok().build();
  }
}
