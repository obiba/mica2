/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import org.obiba.mica.micaConfig.service.VariableTaxonomiesService;
import org.obiba.opal.core.cfg.NoSuchTaxonomyException;
import org.obiba.opal.core.cfg.NoSuchVocabularyException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AbstractTaxonomyResource {

  @Inject
  protected VariableTaxonomiesService variableTaxonomiesService;

  protected List<Opal.TaxonomyDto> getTaxonomyDtos() {
    return variableTaxonomiesService.getTaxonomies().stream().map(Dtos::asDto).collect(Collectors.toList());
  }

  /**
   * Get a summary of all the {@link Taxonomy}s available from Opal master.
   *
   * @return
   */
  protected Opal.TaxonomiesDto getTaxonomySummaryDtos() {
    List<Opal.TaxonomiesDto.TaxonomySummaryDto> summaries = variableTaxonomiesService.getTaxonomies().stream().map(Dtos::asSummaryDto)
      .collect(Collectors.toList());
    return Opal.TaxonomiesDto.newBuilder().addAllSummaries(summaries).build();
  }

  /**
   * Get a summary of the {@link Taxonomy} available from Opal master.
   *
   * @param name the taxonomy name
   * @return
   */
  protected Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomySummaryDto(String name) {
    return Dtos.asSummaryDto(getTaxonomyInternal(name));
  }

  /**
   * Get a summary of all the {@link Taxonomy}s with their
   * {@link Vocabulary}s from Opal master.
   *
   * @return
   */
  protected Opal.TaxonomiesDto getTaxonomyVocabularySummaryDtos() {
    List<Opal.TaxonomiesDto.TaxonomySummaryDto> summaries = variableTaxonomiesService.getTaxonomies().stream().map(Dtos::asVocabularySummaryDto)
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
  protected Opal.TaxonomiesDto.TaxonomySummaryDto getTaxonomyVocabularySummaryDto(String name) {
    return Dtos.asVocabularySummaryDto(getTaxonomyInternal(name));
  }

  /**
   * Get a summary of the {@link Vocabulary} from Opal master.
   *
   * @param name
   * @param vocabularyName
   * @return
   */
  protected Opal.TaxonomiesDto.TaxonomySummaryDto.VocabularySummaryDto getTaxonomyVocabularySummaryDto(String name,
                                                                                                     String vocabularyName) {
    for (Vocabulary voc : getTaxonomyInternal(name).getVocabularies()) {
      if (voc.getName().equals(vocabularyName)) return Dtos.asSummaryDto(voc);
    }
    throw new NoSuchVocabularyException(name, vocabularyName);
  }


  /**
   * Get the {@link Taxonomy} as a Dto from Opal master.
   *
   * @param name
   * @return
   * @throws NoSuchTaxonomyException
   */
  protected Opal.TaxonomyDto getTaxonomyDto(String name) {
    return Dtos.asDto(getTaxonomyInternal(name));
  }

  /**
   * Get the {@link Vocabulary} as a Dto from Opal master.
   *
   * @param name
   * @param vocabularyName
   * @return
   */
  protected Opal.VocabularyDto getTaxonomyVocabularyDto(String name, String vocabularyName) {
    return Dtos.asDto(getTaxonomyInternal(name).getVocabulary(vocabularyName));
  }

  //
  // Private methods
  //

  /**
   * Get the {@link Taxonomy} from Opal master.
   *
   * @param name
   * @return
   * @throws NoSuchTaxonomyException
   */
  private Taxonomy getTaxonomyInternal(String name) {
    Optional<Taxonomy> taxonomy = variableTaxonomiesService.getTaxonomies().stream()
      .filter(taxo -> taxo.getName().equals(name))
      .findFirst();
    if (!taxonomy.isPresent()) {
      throw new NoSuchTaxonomyException(name);
    }
    return taxonomy.get();
  }

}
