package org.obiba.mica.web.model;

import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.micaConfig.domain.AggregationInfo;
import org.obiba.mica.micaConfig.domain.AggregationsConfig;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
class MicaConfigDtos {
  @Inject
  private LocalizedStringDtos localizedStringDtos;

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
  AggregationsConfig fromDto(@NotNull Mica.AggregationsConfigDtoOrBuilder dto){
    AggregationsConfig aggregationsConfig = new AggregationsConfig();
    aggregationsConfig.setStudyAggregations(dto.getStudyList().stream().map(a -> fromDto(a)).collect(Collectors.toList()));
    aggregationsConfig.setVariableAggregations(dto.getVariableList().stream().map(a -> fromDto(a)).collect(Collectors.toList()));

    return aggregationsConfig;
  }

  @NotNull
  Mica.AggregationsConfigDto asDto(@NotNull AggregationsConfig aggregationsConfig) {
    Mica.AggregationsConfigDto.Builder builder = Mica.AggregationsConfigDto.newBuilder();

    aggregationsConfig.getStudyAggregations().forEach(a -> {
      builder.addStudy(asDto(a));
    });

    aggregationsConfig.getVariableAggregations().forEach(a -> {
      builder.addVariable(asDto(a));
    });

    return builder.build();
  }

  @NotNull
  AggregationInfo fromDto(@NotNull Mica.AggregationInfoDtoOrBuilder dto) {
    AggregationInfo aggregation = new AggregationInfo();
    aggregation.setId(dto.getId());
    aggregation.setTitle(localizedStringDtos.fromDto(dto.getTitleList()));

    return aggregation;
  }

  @NotNull
  Mica.AggregationInfoDto asDto(@NotNull AggregationInfo agg) {
    Mica.AggregationInfoDto.Builder builder = Mica.AggregationInfoDto.newBuilder()
      .setId(agg.getId());

    localizedStringDtos.asDto(agg.getTitle()).forEach(t -> builder.addTitle(t));

    return builder.build();
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
