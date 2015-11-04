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
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.opal.web.model.Opal;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/taxonomies")
@RequiresAuthentication
public class TaxonomiesResource {

  @Inject
  private OpalService opalService;

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
}
