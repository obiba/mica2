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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.obiba.opal.web.model.Opal;
import org.springframework.stereotype.Component;

@Component
@Path("/taxonomy/{name}")
public class TaxonomyResource extends AbstractTaxonomyResource {

  @GET
  @Timed
  public Opal.TaxonomyDto getTaxonomy(@PathParam("name") String name) {
    return getTaxonomyDto(name);
  }

  @GET
  @Path("/_summary")
  @Timed
  public Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomySummary(@PathParam("name") String name,
    @QueryParam("vocabularies") @DefaultValue("false") boolean withVocabularies) {
    if(withVocabularies) return getTaxonomyVocabularySummaryDto(name);
    return getTaxonomySummaryDto(name);
  }

  @GET
  @Path("/vocabulary/{vocabulary}")
  @Timed
  public Opal.VocabularyDto getVocabulary(@PathParam("name") String name,
    @PathParam("vocabulary") String vocabularyName) {
    return getTaxonomyVocabularyDto(name, vocabularyName);
  }

  @GET
  @Path("/vocabulary/{vocabulary}/_summary")
  @Timed
  public Opal.TaxonomiesDto.TaxonomySummaryDto.VocabularySummaryDto getVocabularySummary(@PathParam("name") String name,
    @PathParam("vocabulary") String vocabularyName) {
    return getTaxonomyVocabularySummaryDto(name, vocabularyName);
  }

}
