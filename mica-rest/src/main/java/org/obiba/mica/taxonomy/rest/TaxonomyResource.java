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

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.opal.web.model.Opal;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/taxonomy/{name}")
public class TaxonomyResource {

  @Inject
  private OpalService opalService;

  @GET
  @Timed
  public Opal.TaxonomyDto getTaxonomy(@PathParam("name") String name) {
    return opalService.getTaxonomyDto(name);
  }

  @GET
  @Path("/_summary")
  @Timed
  public Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomySummary(@PathParam("name") String name,
    @QueryParam("vocabularies") @DefaultValue("false") boolean withVocabularies) {
    if(withVocabularies) return opalService.getTaxonomyVocabularySummaryDto(name);
    return opalService.getTaxonomySummaryDto(name);
  }

  @GET
  @Path("/vocabulary/{vocabulary}")
  @Timed
  public Opal.VocabularyDto getVocabulary(@PathParam("name") String name,
    @PathParam("vocabulary") String vocabularyName) {
    return opalService.getTaxonomyVocabularyDto(name, vocabularyName);
  }

  @GET
  @Path("/vocabulary/{vocabulary}/_summary")
  @Timed
  public Opal.TaxonomiesDto.TaxonomySummaryDto.VocabularySummaryDto getVocabularySummary(@PathParam("name") String name,
    @PathParam("vocabulary") String vocabularyName) {
    return opalService.getTaxonomyVocabularySummaryDto(name, vocabularyName);
  }

}
