/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/")
@RequiresRoles(Roles.MICA_ADMIN)
public class CacheResource {

  private static final Logger logger = LoggerFactory.getLogger(CacheResource.class);

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

    logger.info("Clear cache [{}]", id);

    switch(id) {
      case "micaConfig" :
        cacheService.clearMicaConfigCache();
        break;
      case "opalTaxonomies":
      case "variableTaxonomies":
        cacheService.clearTaxonomiesCache();
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
