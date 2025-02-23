/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class TaxonomyDtos {

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  public Mica.TaxonomyEntityDto asDto(@NotNull Taxonomy taxonomy, @Nullable String locale) {
    Mica.TaxonomyEntityDto.Builder builder = Mica.TaxonomyEntityDto.newBuilder().setName(taxonomy.getName());
    if(!Strings.isNullOrEmpty(locale)) builder.addAllTitles(localizedStringDtos.asDto(taxonomy.getTitle(), locale)) //
      .addAllDescriptions(localizedStringDtos.asDto(taxonomy.getDescription(), locale)).build();
    return builder.build();
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(@NotNull Vocabulary vocabulary, @Nullable String locale) {
    Mica.TaxonomyEntityDto.Builder builder = Mica.TaxonomyEntityDto.newBuilder().setName(vocabulary.getName());
    if(!Strings.isNullOrEmpty(locale)) builder.addAllTitles(localizedStringDtos.asDto(vocabulary.getTitle(), locale)) //
      .addAllDescriptions(localizedStringDtos.asDto(vocabulary.getDescription(), locale)).build();
    return builder.build();
  }

  @NotNull
  public Mica.TaxonomyEntityDto asDto(@NotNull Term term, @Nullable String locale) {
    Mica.TaxonomyEntityDto.Builder builder = Mica.TaxonomyEntityDto.newBuilder().setName(term.getName());
    if(!Strings.isNullOrEmpty(locale)) builder.addAllTitles(localizedStringDtos.asDto(term.getTitle(), locale)) //
      .addAllDescriptions(localizedStringDtos.asDto(term.getDescription(), locale));
    return builder.build();
  }

}
