/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 *  This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.taxonomy.TaxonomyResolver;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

@Component
@Scope("request")
@Path("/taxonomy/{name}")
@RequiresAuthentication
public class TaxonomySearchResource extends AbstractTaxonomySearchResource {

  @GET
  @Path("/_filter")
  @Timed
  public Opal.TaxonomyDto filterTaxonomy(@PathParam("name") String name, @QueryParam("query") String query) {
    if(Strings.isNullOrEmpty(query)) return opalService.getTaxonomyDto(name);

    String filteredQuery = String.format("taxonomyName:%s AND %s", name, query);
    Map<String, Map<String, List<String>>> taxoNamesMap = TaxonomyResolver
      .asMap(filterVocabularies(filteredQuery), filterTerms(filteredQuery));

    Taxonomy taxonomy = opalService.getTaxonomy(name);
    Opal.TaxonomyDto.Builder tBuilder = Dtos.asDto(taxonomy, false).toBuilder();
    if(taxoNamesMap.isEmpty() || !taxoNamesMap.containsKey(name) || taxoNamesMap.get(name).isEmpty())
      return tBuilder.build();
    populate(tBuilder, taxonomy, taxoNamesMap);
    return tBuilder.build();
  }

  @GET
  @Path("/vocabulary/{vocabulary}/_filter")
  @Timed
  public Opal.VocabularyDto filterVocabulary(@PathParam("name") String name,
    @PathParam("vocabulary") String vocabularyName, @QueryParam("query") String query) {
    if(Strings.isNullOrEmpty(query)) return opalService.getTaxonomyVocabularyDto(name, vocabularyName);

    String filteredQuery = String.format("taxonomyName:%s AND vocabularyName:%s AND %s", name, vocabularyName, query);

    Map<String, Map<String, List<String>>> taxoNamesMap = TaxonomyResolver
      .asMap(filterTerms(filteredQuery));

    Taxonomy taxonomy = opalService.getTaxonomy(name);
    Vocabulary vocabulary = taxonomy.getVocabulary(vocabularyName);
    Opal.VocabularyDto.Builder tBuilder = Dtos.asDto(vocabulary, false).toBuilder();
    if(taxoNamesMap.isEmpty() || !taxoNamesMap.containsKey(name) || taxoNamesMap.get(name).isEmpty() || taxoNamesMap.get(name).get(vocabularyName).isEmpty())
      return tBuilder.build();

    taxoNamesMap.get(name).get(vocabularyName).forEach(term -> tBuilder.addTerms(Dtos.asDto(vocabulary.getTerm(term))));
    return tBuilder.build();
  }

}
