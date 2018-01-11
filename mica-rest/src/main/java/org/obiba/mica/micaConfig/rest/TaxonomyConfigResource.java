/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.rest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.micaConfig.service.TaxonomyConfigService;
import org.obiba.mica.security.Roles;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.springframework.stereotype.Component;

@Component
@Path("/config/{target}/taxonomy")
public class TaxonomyConfigResource {

  @Inject
  TaxonomyConfigService taxonomyConfigService;

  Dtos dtos;

  @Inject
  private CacheService cacheService;


  @GET
  public Response getTaxonomy(@NotNull @PathParam("target") String target) {
    try {
      return Response.ok().entity(Dtos.asDto(taxonomyConfigService.findByTarget(TaxonomyTarget.fromId(target)))).build();
    } catch(IllegalArgumentException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  @PUT
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response update(@NotNull @PathParam("target") String target, @NotNull Opal.TaxonomyDto taxonomyDto) {
    try {
      taxonomyConfigService.update(TaxonomyTarget.fromId(target), Dtos.fromDto(taxonomyDto));
      cacheService.clearMicaConfigCache();
      return Response.ok().build();
    } catch(IllegalArgumentException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

  }
}
