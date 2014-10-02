/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;

@Component
class TaxonomyDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  public Mica.TaxonomyEntityDto asDto(Taxonomy taxonomy) {
    return Mica.TaxonomyEntityDto.newBuilder().setName(taxonomy.getName()) //
        .addAllTitles(localizedStringDtos.asDto(taxonomy.getTitle())) //
        .addAllDescriptions(localizedStringDtos.asDto(taxonomy.getDescription())).build();
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(Vocabulary vocabulary) {
    return Mica.TaxonomyEntityDto.newBuilder().setName(vocabulary.getName()) //
        .addAllTitles(localizedStringDtos.asDto(vocabulary.getTitle())) //
        .addAllDescriptions(localizedStringDtos.asDto(vocabulary.getDescription())).build();
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(Term term) {
    return Mica.TaxonomyEntityDto.newBuilder().setName(term.getName()).addAllTitles(
        localizedStringDtos.asDto(term.getTitle())) //
        .addAllDescriptions(localizedStringDtos.asDto(term.getDescription())).build();
  }

}
