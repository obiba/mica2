package org.obiba.mica.web.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.obiba.mica.micaConfig.service.CacheService;

@Path("/cache")
public class CacheResource {

  @Inject
  private CacheService cacheService;

  @DELETE
  public Response deleteCaches() {
    cacheService.clearCache();
    return Response.ok().build();
  }
}
