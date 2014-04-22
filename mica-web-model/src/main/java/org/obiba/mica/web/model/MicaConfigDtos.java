package org.obiba.mica.web.model;

import java.util.Locale;

import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.MicaConfig;
import org.springframework.stereotype.Component;

@Component
class MicaConfigDtos {

  @NotNull
  Mica.MicaConfigDto asDto(@NotNull MicaConfig config) {
    Mica.MicaConfigDto.Builder builder = Mica.MicaConfigDto.newBuilder() //
        .setName(config.getName()) //
        .setDefaultCharSet(config.getDefaultCharacterSet()) //
        .setPublicURL(config.getPublicUrl());
    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));
    return builder.build();
  }

  @NotNull
  MicaConfig fromDto(@NotNull Mica.MicaConfigDtoOrBuilder dto) {
    MicaConfig config = new MicaConfig();
    config.setName(dto.getName());
    config.setDefaultCharacterSet(dto.getDefaultCharSet());
    config.setPublicUrl(dto.getPublicURL());
    dto.getLanguagesList().forEach(lang -> config.getLocales().add(new Locale(lang)));
    return config;
  }

}
