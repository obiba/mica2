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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@RequiresRoles(Roles.MICA_ADMIN)
public class CacheResource {

  private static final Logger logger = LoggerFactory.getLogger(IllegalArgumentExceptionMapper.class);

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

    logger.info("clear cache [{}]", id);

    switch(id) {
      case "micaConfig" :
        cacheService.clearMicaConfigCache();
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
