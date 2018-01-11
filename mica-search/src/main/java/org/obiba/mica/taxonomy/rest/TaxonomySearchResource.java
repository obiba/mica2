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

import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.opal.core.cfg.NoSuchVocabularyException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;

import static org.obiba.mica.taxonomy.TaxonomyResolver.asMap;

@Component
@Scope("request")
@Path("/taxonomy/{name}")
@RequiresAuthentication
public class TaxonomySearchResource extends AbstractTaxonomySearchResource {

  @GET
  @Path("/_filter")
  @Timed
  public Opal.TaxonomyDto filterTaxonomy(@PathParam("name") String name,
    @QueryParam("target") @DefaultValue("variable") String target, @QueryParam("query") String query,
    @QueryParam("locale") String locale) {
    TaxonomyTarget taxonomyTarget = getTaxonomyTarget(target);
    if(taxonomyTarget.equals(TaxonomyTarget.TAXONOMY)) return Dtos.asDto(getTaxonomy(taxonomyTarget, name));

    List<String> filteredVocabularies = filterVocabularies(taxonomyTarget, query, locale);

    Map<String, Map<String, List<String>>> taxoNamesMap = asMap(filteredVocabularies, filterTerms(taxonomyTarget, query, locale, filteredVocabularies));

    Taxonomy taxonomy = getTaxonomy(taxonomyTarget, name);
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
    @PathParam("vocabulary") String vocabularyName, @QueryParam("target") @DefaultValue("variable") String target,
    @QueryParam("query") String query, @QueryParam("locale") String locale) {
    TaxonomyTarget taxonomyTarget = getTaxonomyTarget(target);
    if(taxonomyTarget.equals(TaxonomyTarget.TAXONOMY))  return Dtos.asDto(getTaxonomy(taxonomyTarget, name).getVocabulary(vocabularyName));

    List<String> foundVocabularies = filterVocabularies(taxonomyTarget, String.format("name:%s", vocabularyName), locale);

    if (foundVocabularies.isEmpty()) {
      throw new NoSuchVocabularyException(name, vocabularyName);
    }

    Taxonomy taxonomy = getTaxonomy(taxonomyTarget, name);
    Vocabulary vocabulary = taxonomy.getVocabulary(vocabularyName);
    Opal.VocabularyDto.Builder tBuilder = Dtos.asDto(vocabulary, false).toBuilder();

    String filteredQuery = String.format(Strings.isNullOrEmpty(query)
      ? "taxonomyName:%s AND vocabularyName:%s"
      : "taxonomyName:%s AND vocabularyName:%s AND (%s)", name, vocabularyName, query);

    Map<String, Map<String, List<String>>> taxoNamesMap = asMap(filterTerms(taxonomyTarget, filteredQuery, locale, null));

    if(taxoNamesMap.isEmpty() || !taxoNamesMap.containsKey(name) || taxoNamesMap.get(name).isEmpty() ||
      taxoNamesMap.get(name).get(vocabularyName).isEmpty()) return tBuilder.build();

    taxoNamesMap.get(name).get(vocabularyName).forEach(term -> tBuilder.addTerms(Dtos.asDto(vocabulary.getTerm(term))));
    return tBuilder.build();
  }

}
