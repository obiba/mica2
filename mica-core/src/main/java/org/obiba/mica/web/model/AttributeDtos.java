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

import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.Attribute;
import org.springframework.stereotype.Component;

@Component
class AttributeDtos {


  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @NotNull
  Mica.AttributeDto asDto(@NotNull Attribute attribute) {
    Mica.AttributeDto.Builder builder = Mica.AttributeDto.newBuilder();
    builder.setName(attribute.getName());
    if(attribute.getNamespace() != null) builder.setNamespace(attribute.getNamespace());
    if(attribute.getValues() != null) {
      builder.addAllValues(localizedStringDtos.asDto(attribute.getValues()));
    }

    return builder.build();
  }

  @NotNull
  Attribute fromDto(@NotNull Mica.AttributeDto dto) {
    Attribute.Builder builder = Attribute.Builder.newAttribute(dto.getName());
    if(dto.hasNamespace()) builder.namespace(dto.getNamespace());
    builder.values(localizedStringDtos.fromDto(dto.getValuesList()));
    return builder.build();
  }

}
