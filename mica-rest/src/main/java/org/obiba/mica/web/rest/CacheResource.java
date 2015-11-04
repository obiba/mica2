package org.obiba.mica.web.rest;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.security.Roles;

@Path("/")
@RequiresRoles(Roles.MICA_ADMIN)
public class CacheResource {

  @Inject
  private CacheService cacheService;

  @Path("/caches")
  @DELETE
  public Response deleteCaches() {
    cacheService.clearAllCaches();
    return Response.ok().build();
  }

  @Path("/cache/{id}")
  @DELETE
  public Response deleteCache(@PathParam("id") String id) {
    switch(id) {
      case "micaConfig" :
        cacheService.clearMicaConfigCache();
        break;
      case "agateSubjects":
        cacheService.clearAgateSubjectsCache();
        break;
      case "opalTaxonomies":
        cacheService.clearOpalTaxonomiesCache();
        break;
      case "aggregationsMetadata":
        cacheService.clearAggregationsMetadataCache();
        break;
      case "datasetVariables":
        cacheService.clearDatasetVariablesCache();
        break;
      case "authorization":
        cacheService.clearAuthorizationCache();
        break;
      default:
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.ok().build();
  }

  @Path("/cache/{id}")
  @PUT
  public Response buildCache(@PathParam("id") String id) {
    switch(id) {
      case "datasetVariables":
        cacheService.buildDatasetVariablesCache();
        break;
      default:
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    return Response.ok().build();
  }
}
