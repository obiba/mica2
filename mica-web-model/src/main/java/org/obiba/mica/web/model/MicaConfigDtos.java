package org.obiba.mica.web.model;

import java.util.Locale;

import javax.validation.constraints.NotNull;

import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.MicaConfig;
import org.obiba.mica.micaConfig.OpalCredential;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class MicaConfigDtos {

  @NotNull
  Mica.MicaConfigDto asDto(@NotNull MicaConfig config) {
    Mica.MicaConfigDto.Builder builder = Mica.MicaConfigDto.newBuilder() //
        .setName(config.getName()) //
        .setDefaultCharSet(config.getDefaultCharacterSet());
    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));
    if(!Strings.isNullOrEmpty(config.getPublicUrl())) {
      builder.setPublicUrl(config.getPublicUrl());
    }
    builder.setOpal(config.getOpal());
    return builder.build();
  }

  @NotNull
  MicaConfig fromDto(@NotNull Mica.MicaConfigDtoOrBuilder dto) {
    MicaConfig config = new MicaConfig();
    config.setName(dto.getName());
    config.setDefaultCharacterSet(dto.getDefaultCharSet());
    if(dto.hasPublicUrl()) config.setPublicUrl(dto.getPublicUrl());
    dto.getLanguagesList().forEach(lang -> config.getLocales().add(new Locale(lang)));
    config.setOpal(dto.getOpal());
    return config;
  }

  @NotNull
  Mica.OpalCredentialDto asDto(@NotNull OpalCredential credential) {
    Mica.OpalCredentialDto.Builder builder = Mica.OpalCredentialDto.newBuilder()
      .setType(credential.getAuthType() == AuthType.USERNAME ? Mica.OpalCredentialType.USERNAME : Mica.OpalCredentialType.PUBLIC_KEY_CERTIFICATE)
      .setOpalUrl(credential.getOpalUrl());

    if(!Strings.isNullOrEmpty(credential.getUsername())) builder.setUsername(credential.getUsername());

    return builder.build();
  }
}
