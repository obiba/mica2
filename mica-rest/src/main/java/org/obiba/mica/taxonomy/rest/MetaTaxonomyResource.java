/*
 * Copyright (c) 2023 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;


import com.codahale.metrics.annotation.Timed;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Component
@Path("/meta-taxonomy")
public class MetaTaxonomyResource {

  @Inject
  private TaxonomiesService taxonomiesService;

  @GET
  @Timed
  public Opal.TaxonomyDto getTaxonomy() {
    return Dtos.asDto(taxonomiesService.getTaxonomyTaxonomy());
  }

  @PUT
  @Path("/{target}/{taxonomy}/_move")
  public Response up(@PathParam("target") @DefaultValue("variable") String target, @PathParam("taxonomy") String taxonomy, @QueryParam("dir") String dir) {
    taxonomiesService.moveTaxonomy(TaxonomyTarget.fromId(target), taxonomy, "up".equals(dir));
    return Response.ok().build();
  }

  @PUT
  @Path("/{target}/{taxonomy}/_attribute")
  public Response hide(@PathParam("target") @DefaultValue("variable") String target, @PathParam("taxonomy") String taxonomy, @QueryParam("name") String name, @QueryParam("value") String value) {
    taxonomiesService.setTaxonomyAttribute(TaxonomyTarget.fromId(target), taxonomy, name, value);
    return Response.ok().build();
  }

}
