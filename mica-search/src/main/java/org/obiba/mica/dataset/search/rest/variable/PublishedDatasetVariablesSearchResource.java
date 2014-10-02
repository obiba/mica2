/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.micaConfig.OpalService;
import org.obiba.mica.search.JoinQueryExecutor;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Search for variables in the published variable index.
 */
@Path("/variables")
@RequiresAuthentication
@Scope("request")
public class PublishedDatasetVariablesSearchResource {

  @Inject
  private OpalService opalService;

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Dtos dtos;

  @POST
  @Timed
  @Path("_search")
  public MicaSearch.JoinQueryResultDto list(MicaSearch.JoinQueryDto joinQueryDto) throws IOException {
    return joinQueryExecutor.query(JoinQueryExecutor.QueryType.VARIABLE, joinQueryDto);
  }

  /**
   * Get the frequency of each taxonomy terms, based on variables aggregation results.
   *
   * @param taxonomyNames
   * @param joinQueryDto
   * @return
   * @throws IOException
   */
  @POST
  @Timed
  @Path("_coverage")
  public MicaSearch.TaxonomiesCoverageDto coverage(@QueryParam("taxonomy") List<String> taxonomyNames,
      MicaSearch.JoinQueryDto joinQueryDto) throws IOException {

    MicaSearch.JoinQueryResultDto result = joinQueryExecutor.query(JoinQueryExecutor.QueryType.VARIABLE, joinQueryDto);
    List<MicaSearch.AggregationResultDto> aggregations = result.getVariableResultDto().getAggsList();

    Map<String, Map<String, Integer>> aggsMap = Maps.newHashMap();
    aggregations.forEach(agg -> {
      String name = agg.getAggregation();
      List<MicaSearch.TermsAggregationResultDto> results = agg.getExtension(MicaSearch.TermsAggregationResultDto.terms);
      if(results != null && !results.isEmpty() && name.startsWith("attributes-") && name.endsWith("-und")) {
        String key = name.replaceAll("^attributes-", "").replaceAll("-und$", "");
        if(!aggsMap.containsKey(key)) aggsMap.put(key, Maps.newHashMap());
        results.forEach(res -> aggsMap.get(key).put(res.getKey(), res.getCount()));
      }
    });

    List<MicaSearch.TaxonomyCoverageDto> coverages = Lists.newArrayList();
    getTaxonomies().stream().filter(
        taxonomy -> taxonomyNames == null || taxonomyNames.isEmpty() || taxonomyNames.contains(taxonomy.getName()))
        .forEach(taxonomy -> addTaxonomyCoverage(coverages, taxonomy, aggsMap));

    MicaSearch.TaxonomiesCoverageDto.Builder builder = MicaSearch.TaxonomiesCoverageDto.newBuilder()//
        .setTotalCount(result.getVariableResultDto().getTotalCount()) //
        .setTotalHits(result.getVariableResultDto().getTotalHits()) //
        .addAllTaxonomies(coverages);

    return builder.build();
  }

  private void addTaxonomyCoverage(List<MicaSearch.TaxonomyCoverageDto> coverages, Taxonomy taxonomy,
      Map<String, Map<String, Integer>> aggsMap) {
    if(taxonomy.hasVocabularies()) {
      MicaSearch.TaxonomyCoverageDto.Builder taxoBuilder = MicaSearch.TaxonomyCoverageDto.newBuilder();
      taxoBuilder.setTaxonomy(dtos.asDto(taxonomy));
      taxonomy.getVocabularies().forEach(vocabulary -> addVocabularyCoverage(taxoBuilder, vocabulary,
          aggsMap.get(AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()))));
      coverages.add(taxoBuilder.build());
    }
  }

  private void addVocabularyCoverage(MicaSearch.TaxonomyCoverageDto.Builder taxoBuilder, Vocabulary vocabulary,
      Map<String, Integer> counts) {
    if(vocabulary.hasTerms()) {
      MicaSearch.VocabularyCoverageDto.Builder vocBuilder = MicaSearch.VocabularyCoverageDto.newBuilder();
      vocBuilder.setVocabulary(dtos.asDto(vocabulary));
      vocabulary.getTerms().forEach(term -> addTermCoverage(vocBuilder, term, counts));
      // only one term can be applied at a time, then the sum of the term counts is the number of variables
      // that cover this vocabulary
      if(!vocabulary.isRepeatable() && counts != null) {
        vocBuilder.setCount(counts.values().stream().mapToInt(c -> c).sum());
      }
      taxoBuilder.addVocabularies(vocBuilder);
    }
  }

  private void addTermCoverage(MicaSearch.VocabularyCoverageDto.Builder vocBuilder, Term term,
      Map<String, Integer> counts) {
    MicaSearch.TermCoverageDto.Builder termBuilder = MicaSearch.TermCoverageDto.newBuilder();
    termBuilder.setTerm(dtos.asDto(term));
    termBuilder.setCount(0);
    if(counts != null && counts.containsKey(term.getName())) termBuilder.setCount(counts.get(term.getName()));
    vocBuilder.addTerms(termBuilder);
  }

  @NotNull
  private List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }
}
