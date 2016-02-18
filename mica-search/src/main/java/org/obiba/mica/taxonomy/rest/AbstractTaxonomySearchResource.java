/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.index.IndexNotFoundException;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.taxonomy.EsTaxonomyTermService;
import org.obiba.mica.taxonomy.EsTaxonomyVocabularyService;
import org.obiba.mica.taxonomy.TaxonomyTarget;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import sun.util.locale.LanguageTag;

public class AbstractTaxonomySearchResource {

  private static final int MAX_SIZE = 10000;

  private static final String DEFAULT_SORT = "id";

  private static final String[] VOCABULARY_FIELDS = { "title", "description" };

  private static final String[] TERM_FIELDS = { "title", "description", "keywords" };

  @Inject
  private EsTaxonomyTermService esTaxonomyTermService;

  @Inject
  private EsTaxonomyVocabularyService esTaxonomyVocabularyService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private OpalService opalService;

  protected void populate(Opal.TaxonomyDto.Builder tBuilder, Taxonomy taxonomy,
    Map<String, Map<String, List<String>>> taxoNamesMap) {
    taxonomy.getVocabularies().stream().filter(v -> taxoNamesMap.get(taxonomy.getName()).containsKey(v.getName()))
      .forEach(voc -> {
        Opal.VocabularyDto.Builder vBuilder = Dtos.asDto(voc, false).toBuilder();
        List<String> termNames = taxoNamesMap.get(taxonomy.getName()).get(voc.getName());
        if (voc.hasTerms()) {
          if(termNames.isEmpty()) vBuilder.addAllTerms(voc.getTerms().stream().map(Dtos::asDto).collect(Collectors.toList()));
          else voc.getTerms().stream().filter(t -> termNames.contains(t.getName())).forEach(term -> vBuilder.addTerms(Dtos.asDto(term)));
        }
        tBuilder.addVocabularies(vBuilder);
      });
  }

  protected List<String> filterVocabularies(TaxonomyTarget target, String query, String locale) {
    try {
      return esTaxonomyVocabularyService
        .find(0, MAX_SIZE, DEFAULT_SORT, "asc", null, getTargettedQuery(target, query),
          getFields(locale, VOCABULARY_FIELDS)).getList();
    } catch(IndexNotFoundException e) {
      initTaxonomies();
      // for a 404 response
      throw new NoSuchElementException();
    }
  }

  protected List<String> filterTerms(TaxonomyTarget target, String query, String locale) {
    try {
      return esTaxonomyTermService
        .find(0, MAX_SIZE, DEFAULT_SORT, "asc", null, getTargettedQuery(target, query), getFields(locale, TERM_FIELDS))
        .getList();
    } catch(IndexNotFoundException e) {
      initTaxonomies();
      // for a 404 response
      throw new NoSuchElementException();
    }
  }

  protected List<Taxonomy> getTaxonomies(TaxonomyTarget target) {
    switch(target) {
      case NETWORK:
        return Lists.newArrayList(micaConfigService.getNetworkTaxonomy());
      case STUDY:
        return Lists.newArrayList(micaConfigService.getStudyTaxonomy());
      case DATASET:
        return Lists.newArrayList(micaConfigService.getDatasetTaxonomy());
      default:
        List<Taxonomy> taxonomies = opalService.getTaxonomies();
        taxonomies.add(micaConfigService.getVariableTaxonomy());
        return taxonomies;
    }
  }

  protected Taxonomy getTaxonomy(TaxonomyTarget target, String name) {
    switch(target) {
      case NETWORK:
        return micaConfigService.getNetworkTaxonomy();
      case STUDY:
        return micaConfigService.getStudyTaxonomy();
      case DATASET:
        return micaConfigService.getDatasetTaxonomy();
      default:
        Taxonomy varTaxonomy = micaConfigService.getVariableTaxonomy();
        if (varTaxonomy.getName().equals(name)) return varTaxonomy;
        return opalService.getTaxonomy(name);
    }
  }

  protected TaxonomyTarget getTaxonomyTarget(String target) {
    try {
      return TaxonomyTarget.valueOf(target.toUpperCase());
    } catch(Exception e) {
      throw new NoSuchElementException("No such taxonomy target: " + target);
    }
  }

  private String getTargettedQuery(TaxonomyTarget target, String query) {
    return String.format("target:%s AND (%s)", target.name(), query);
  }

  private List<String> getFields(String locale, String... fieldNames) {
    List<String> fields = Lists.newArrayList("name.analyzed");
    List<String> locales = Lists.newArrayList();

    if(Strings.isNullOrEmpty(locale)) {
      locales.addAll(micaConfigService.getConfig().getLocalesAsString());
      locales.add(LanguageTag.UNDETERMINED);
    } else {
      locales.add(locale);
    }

    locales.forEach(local -> Arrays.stream(fieldNames).forEach(f -> fields.add(f + "." + local + ".analyzed")));

    return fields;
  }

  /**
   * Populate taxonomies cache and trigger taxonomies indexing.
   */
  private void initTaxonomies() {
    opalService.getTaxonomies();
  }
}
