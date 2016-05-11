/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.CacheService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.security.Roles;
import org.obiba.opal.web.model.Opal;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;

@Component
@Path("/taxonomies")
@RequiresAuthentication
public class TaxonomiesResource {

  @Inject
  private OpalService opalService;

  @Inject
  private CacheService cacheService;

  @Inject
  private EventBus eventBus;

  @GET
  @Timed
  public List<Opal.TaxonomyDto> getTaxonomies() {
    return opalService.getTaxonomyDtos();
  }

  @GET
  @Path("/_summary")
  @Timed
  public Opal.TaxonomiesDto getTaxonomySummaries(
    @QueryParam("vocabularies") @DefaultValue("false") boolean withVocabularies) {
    if(withVocabularies) return opalService.getTaxonomyVocabularySummaryDtos();
    return opalService.getTaxonomySummaryDtos();
  }

  @PUT
  @Path("/_index")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateIndices() {
    cacheService.clearOpalTaxonomiesCache();
    eventBus.post(new TaxonomiesUpdatedEvent());
    return Response.noContent().build();
  }
}
