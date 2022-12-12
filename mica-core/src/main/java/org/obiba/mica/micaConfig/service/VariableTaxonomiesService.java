/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;


import com.google.common.collect.Lists;
import org.obiba.opal.core.cfg.NoSuchTaxonomyException;
import org.obiba.opal.core.cfg.NoSuchVocabularyException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Access to the variable taxonomies, from the different providers, including opal, local files and plugins.
 */
@Component
public class VariableTaxonomiesService {

  @Inject
  private OpalService opalService;

  public List<Taxonomy> getTaxonomies() {
    Map<String, Taxonomy> taxonomies = opalService.getTaxonomiesInternal();
    List<Taxonomy> taxonomyList = Lists.newArrayList(taxonomies.values());
    Collections.sort(taxonomyList, Comparator.comparing(TaxonomyEntity::getName));
    return taxonomyList;
  }

  /**
   * Get a not-null list of taxonomies.
   *
   * @return
   */
  @NotNull
  public List<Taxonomy> getSafeTaxonomies() {
    List<Taxonomy> taxonomies = null;

    try {
      taxonomies = getTaxonomies();
    } catch(Exception e) {
      // ignore
    }

    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  public List<Opal.TaxonomyDto> getTaxonomyDtos() {
    return getTaxonomies().stream().map(Dtos::asDto).collect(Collectors.toList());
  }

  /**
   * Get a summary of all the {@link Taxonomy}s available from Opal master.
   *
   * @return
   */
  public Opal.TaxonomiesDto getTaxonomySummaryDtos() {
    List<Opal.TaxonomiesDto.TaxonomySummaryDto> summaries = getTaxonomies().stream().map(Dtos::asSummaryDto)
      .collect(Collectors.toList());

    return Opal.TaxonomiesDto.newBuilder().addAllSummaries(summaries).build();
  }

  /**
   * Get a summary of the {@link Taxonomy} available from Opal master.
   *
   * @param name the taxonomy name
   * @return
   */
  public Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomySummaryDto(String name) {
    return Dtos.asSummaryDto(getTaxonomy(name));
  }

  /**
   * Get a summary of all the {@link Taxonomy}s with their
   * {@link Vocabulary}s from Opal master.
   *
   * @return
   */
  public Opal.TaxonomiesDto getTaxonomyVocabularySummaryDtos() {
    List<Opal.TaxonomiesDto.TaxonomySummaryDto> summaries = getTaxonomies().stream().map(Dtos::asVocabularySummaryDto)
      .collect(Collectors.toList());

    return Opal.TaxonomiesDto.newBuilder().addAllSummaries(summaries).build();
  }

  /**
   * Get a summary of the {@link Taxonomy} with its
   * {@link Vocabulary}s from Opal master.
   *
   * @param name the taxonomy name
   * @return
   */
  public Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomyVocabularySummaryDto(String name) {
    return Dtos.asVocabularySummaryDto(getTaxonomy(name));
  }

  /**
   * Get a summary of the {@link Vocabulary} from Opal master.
   *
   * @param name
   * @param vocabularyName
   * @return
   */
  public Opal.TaxonomiesDto.TaxonomySummaryDto.VocabularySummaryDto getTaxonomyVocabularySummaryDto(String name,
                                                                                                    String vocabularyName) {
    for (Vocabulary voc : getTaxonomy(name).getVocabularies()) {
      if (voc.getName().equals(vocabularyName)) return Dtos.asSummaryDto(voc);
    }
    throw new NoSuchVocabularyException(name, vocabularyName);
  }

  /**
   * Get the {@link Taxonomy} from Opal master.
   *
   * @param name
   * @return
   * @throws NoSuchTaxonomyException
   */
  public Taxonomy getTaxonomy(String name) {
    return Dtos.fromDto(getTaxonomyDto(name));
  }

  /**
   * Get the {@link Taxonomy} as a Dto from Opal master.
   *
   * @param name
   * @return
   * @throws NoSuchTaxonomyException
   */
  public Opal.TaxonomyDto getTaxonomyDto(String name) {
    Map<String, Taxonomy> taxonomies = opalService.getTaxonomiesInternal();

    if (!taxonomies.containsKey(name)) {
      throw new NoSuchTaxonomyException(name);
    }

    return Dtos.asDto(taxonomies.get(name));
  }

  /**
   * Get the {@link Vocabulary} as a Dto from Opal master.
   *
   * @param name
   * @param vocabularyName
   * @return
   */
  public Opal.VocabularyDto getTaxonomyVocabularyDto(String name, String vocabularyName) {
    Map<String, Taxonomy> taxonomies = opalService.getTaxonomiesInternal();

    if (!taxonomies.containsKey(name)) {
      throw new NoSuchTaxonomyException(name);
    }

    return Dtos.asDto(taxonomies.get(name).getVocabulary(vocabularyName));
  }

}
