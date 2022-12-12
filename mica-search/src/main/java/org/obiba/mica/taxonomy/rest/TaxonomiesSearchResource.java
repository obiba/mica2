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
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.obiba.mica.security.SubjectUtils;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.taxonomy.TaxonomyResolver;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;

@Component
@Scope("request")
@Path("/taxonomies")
public class TaxonomiesSearchResource extends AbstractTaxonomySearchResource {

  private static final Logger logger = LoggerFactory.getLogger(TaxonomiesSearchResource.class);

  @Inject
  private TaxonomiesService taxonomiesService;

  @GET
  @Path("/_filter")
  @Timed
  public List<Opal.TaxonomyDto> filter(@Context HttpServletRequest request, @QueryParam("target") @DefaultValue("variable") String target,
                                       @QueryParam("query") String query, @QueryParam("locale") String locale) {
    TaxonomyTarget taxonomyTarget = getTaxonomyTarget(target);

    List<String> filteredVocabularies = filterVocabularies(taxonomyTarget, query, locale);

    Map<String, Map<String, List<String>>> taxoNamesMap = TaxonomyResolver
      .asMap(filteredVocabularies, filterTerms(taxonomyTarget, query, locale, filteredVocabularies));
    List<Opal.TaxonomyDto> results = Lists.newArrayList();
    getTaxonomies(taxonomyTarget).stream().filter(t -> taxoNamesMap.containsKey(t.getName())).forEach(taxo -> {
      Opal.TaxonomyDto.Builder tBuilder = Dtos.asDto(taxo, false).toBuilder();
      populate(tBuilder, taxo, taxoNamesMap, SubjectUtils.getAnonymousUserId(request));
      results.add(tBuilder.build());
    });

      return results;
  }

  @GET
  @Path("/_search")
  @Timed
  public List<Opal.TaxonomyBundleDto> search(@Context HttpServletRequest request, @QueryParam("query") String query, @QueryParam("locale") String locale,
    @Nullable @QueryParam("target") String target) {

    logger.debug("TaxonomiesSearchResource#search called with query [%s], locale [%s] and target [%s]", query, locale, target);

    List<Opal.TaxonomyBundleDto> results = Lists.newArrayList();

    List<TaxonomyTarget> targets = target == null
      ? taxonomiesService.getTaxonomyTaxonomy().getVocabularies().stream()
      .map(t -> TaxonomyTarget.valueOf(t.getName().toUpperCase())).collect(Collectors.toList())
      : Lists.newArrayList(TaxonomyTarget.valueOf(target.toUpperCase()));

    targets.forEach(t -> filter(request, t.name(), query, locale).stream()
      .map(taxo -> Opal.TaxonomyBundleDto.newBuilder().setTarget(t.name().toLowerCase()).setTaxonomy(taxo).build())
      .forEach(results::add));
    return results;
  }

}
