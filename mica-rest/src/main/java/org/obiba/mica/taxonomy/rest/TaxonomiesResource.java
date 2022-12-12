/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.mica.micaConfig.service.VariableTaxonomiesService;
import org.obiba.mica.security.Roles;
import org.obiba.opal.web.model.Opal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Component
@Path("/taxonomies")
public class TaxonomiesResource {

  private static final Logger logger = LoggerFactory.getLogger(TaxonomiesResource.class);

  @Inject
  private VariableTaxonomiesService variableTaxonomiesService;

  @Inject
  private EventBus eventBus;

  @GET
  @Timed
  public List<Opal.TaxonomyDto> getTaxonomies() {
    return variableTaxonomiesService.getTaxonomyDtos();
  }

  @GET
  @Path("/_summary")
  @Timed
  public Opal.TaxonomiesDto getTaxonomySummaries(
    @QueryParam("vocabularies") @DefaultValue("false") boolean withVocabularies) {
    if(withVocabularies) return variableTaxonomiesService.getTaxonomyVocabularySummaryDtos();
    return variableTaxonomiesService.getTaxonomySummaryDtos();
  }

  @PUT
  @Path("/_index")
  @Timed
  @RequiresRoles(Roles.MICA_ADMIN)
  public Response updateIndices() {
    logger.info("clear and rebuild cache opalTaxonomies url /taxonomies/_index");
    eventBus.post(new TaxonomiesUpdatedEvent());
    return Response.noContent().build();
  }
}
