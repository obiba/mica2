package org.obiba.mica.web.model;

import java.util.Locale;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attribute;
import org.springframework.stereotype.Component;

@Component
public class AttributeDtos {

  @NotNull
  public Mica.AttributeDto asDto(@NotNull Attribute attribute) {
    Mica.AttributeDto.Builder builder = Mica.AttributeDto.newBuilder();
    builder.setName(attribute.getName());
    if(attribute.getNamespace() != null) builder.setNamespace(attribute.getNamespace());
    if(attribute.getLocale() != null) builder.setLocale(attribute.getLocale().toString());
    if(attribute.getValue() != null) builder.setValue(attribute.getValue());
    return builder.build();
  }

  @NotNull
  public Attribute fromDto(@NotNull Mica.AttributeDto dto) {
    Attribute.Builder builder = Attribute.Builder.newAttribute(dto.getName()).value(dto.getValue());
    if(dto.hasNamespace()) builder.namespace(dto.getNamespace());
    if(dto.hasLocale()) builder.locale(new Locale.Builder().setLanguage(dto.getLocale()).build());
    return builder.build();
  }

}
