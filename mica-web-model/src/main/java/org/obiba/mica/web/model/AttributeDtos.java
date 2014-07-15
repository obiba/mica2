package org.obiba.mica.web.model;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attribute;
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
