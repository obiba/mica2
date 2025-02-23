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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.dataset.service.VariableSetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.TaxonomyNotFoundException;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.taxonomy.EsTaxonomyTermService;
import org.obiba.mica.taxonomy.EsTaxonomyVocabularyService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;

import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class AbstractTaxonomySearchResource {

  private static final int MAX_SIZE = 10000;

  private static final String DEFAULT_SORT = "id";

  private static final String[] VOCABULARY_FIELDS = {"title", "description"};

  private static final String[] TERM_FIELDS = {"title", "description", "keywords"};

  private static final String LANGUAGE_TAG_UNDETERMINED = "und";

  @Inject
  private EsTaxonomyTermService esTaxonomyTermService;

  @Inject
  private EsTaxonomyVocabularyService esTaxonomyVocabularyService;

  @Inject
  private TaxonomiesService taxonomiesService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private VariableSetService variableSetService;

  protected void populate(Opal.TaxonomyDto.Builder tBuilder, Taxonomy taxonomy,
                          Map<String, Map<String, List<String>>> taxoNamesMap, String anonymousUserId) {

    taxonomy.getVocabularies().stream()
      .filter(v -> taxoNamesMap.get(taxonomy.getName()).containsKey(v.getName()))
      .forEach(voc -> {
        Opal.VocabularyDto.Builder vBuilder = Dtos.asDto(voc, false).toBuilder();
        List<String> termNames = taxoNamesMap.get(taxonomy.getName()).get(voc.getName());
        if (voc.hasTerms()) {
          List<Term> terms = voc.getTerms();

          // for now, sets are available for variable documents only
          if (taxonomy.getName().equals("Mica_" + TaxonomyTarget.VARIABLE.asId()) && voc.getName().equals("sets")) {
            List<DocumentSet> sets = SecurityUtils.getSubject().isAuthenticated() ?
              variableSetService.getAllCurrentUser() :
              variableSetService.getAllAnonymousUser(anonymousUserId);
            List<String> allSetsCurrentUser = sets.stream().map(DocumentSet::getId).collect(Collectors.toList());

            if (allSetsCurrentUser.size() > 0) {
              terms = voc.getTerms().stream().filter(term -> allSetsCurrentUser.contains(term.getName())).collect(Collectors.toList());
            } else {
              terms = Lists.newArrayList();
            }
          }

          if (termNames.isEmpty())
            vBuilder.addAllTerms(terms.stream().map(Dtos::asDto).collect(Collectors.toList()));
          else terms.stream().filter(t -> termNames.contains(t.getName()))
            .forEach(term -> vBuilder.addTerms(Dtos.asDto(term)));
        }
        tBuilder.addVocabularies(vBuilder);
      });
  }

  protected List<String> filterVocabularies(TaxonomyTarget target, String query, String locale) {
    try {
      return esTaxonomyVocabularyService.find(0, MAX_SIZE, DEFAULT_SORT, "asc", null, getTargetedQuery(target, query),
        getFields(locale, VOCABULARY_FIELDS), null, null).getList();
    } catch (Exception e) {
      initTaxonomies();
      // for a 404 response
      throw new NoSuchElementException();
    }
  }

  protected List<String> filterTerms(TaxonomyTarget target, String query, String locale, List<String> vocabularies) {
    try {
      if (vocabularies != null && vocabularies.size() > 0) {
        // filter on vocabulary names; remove taxonomy prefixes ('Mica_study:')
        String vocabulariesQuery = vocabularies.stream()
          .map(v -> String.format("vocabularyName:%s", v.replaceAll("^([^\\:]+):", "")))
          .collect(Collectors.joining(" AND "));
        query = Strings.isNullOrEmpty(query) ? vocabulariesQuery : query + " " + vocabulariesQuery;
      }

      return esTaxonomyTermService
        .find(0, MAX_SIZE, DEFAULT_SORT, "asc", null, getTargetedQuery(target, query), getFields(locale, TERM_FIELDS))
        .getList();
    } catch (Exception e) {
      initTaxonomies();
      // for a 404 response
      throw new NoSuchElementException();
    }
  }

  protected List<Taxonomy> getTaxonomies(TaxonomyTarget target) {
    switch (target) {
      case NETWORK:
        return Lists.newArrayList(taxonomiesService.getNetworkTaxonomy());
      case STUDY:
        return Lists.newArrayList(taxonomiesService.getStudyTaxonomy());
      case DATASET:
        return Lists.newArrayList(taxonomiesService.getDatasetTaxonomy());
      case TAXONOMY:
        return Lists.newArrayList(taxonomiesService.getTaxonomyTaxonomy());
      default:
        return taxonomiesService.getAllVariableTaxonomies();
    }
  }

  protected Taxonomy getTaxonomy(TaxonomyTarget target, String name) {
    switch (target) {
      case NETWORK:
        return taxonomiesService.getNetworkTaxonomy();
      case STUDY:
        return taxonomiesService.getStudyTaxonomy();
      case DATASET:
        return taxonomiesService.getDatasetTaxonomy();
      case TAXONOMY:
        return taxonomiesService.getTaxonomyTaxonomy();
      default:
        Taxonomy foundTaxonomy = taxonomiesService.getAllVariableTaxonomies().stream().filter(taxonomy -> taxonomy.getName().equals(name))
          .findFirst().orElse(null);

        if (foundTaxonomy == null) {
          throw new TaxonomyNotFoundException(name);
        }

        return foundTaxonomy;
    }
  }

  protected TaxonomyTarget getTaxonomyTarget(String target) {
    try {
      return TaxonomyTarget.valueOf(target.toUpperCase());
    } catch (Exception e) {
      throw new NoSuchElementException("No such taxonomy target: " + target);
    }
  }

  private String getTargetedQuery(TaxonomyTarget target, String query) {
    return String.format(Strings.isNullOrEmpty(query) ? "target:%s" : "target:%s AND (%s)", target.name(), query);
  }

  private List<String> getFields(String locale, String... fieldNames) {
    List<String> fields = Lists.newArrayList("name.analyzed");
    List<String> locales = Lists.newArrayList();

    if (Strings.isNullOrEmpty(locale)) {
      locales.addAll(micaConfigService.getConfig().getLocalesAsString());
      locales.add(LANGUAGE_TAG_UNDETERMINED);
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
    taxonomiesService.getVariableTaxonomies();
  }
}
