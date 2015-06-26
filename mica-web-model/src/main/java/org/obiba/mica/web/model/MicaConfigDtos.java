package org.obiba.mica.web.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.AggregationInfo;
import org.obiba.mica.micaConfig.domain.AggregationsConfig;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
class MicaConfigDtos {
  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private AttachmentDtos attachmentDtos;

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

    if(config.getMicaVersion() != null) {
      builder.setVersion(config.getMicaVersion().toString());
    }

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
  AggregationsConfig fromDto(@NotNull Mica.AggregationsConfigDtoOrBuilder dto) {
    AggregationsConfig aggregationsConfig = new AggregationsConfig();
    aggregationsConfig.setStudyAggregations(dto.getStudyList().stream().map(a -> fromDto(a)).collect(toList()));
    aggregationsConfig.setVariableAggregations(dto.getVariableList().stream().map(a -> fromDto(a)).collect(toList()));

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
    Mica.AggregationInfoDto.Builder builder = Mica.AggregationInfoDto.newBuilder().setId(agg.getId());

    localizedStringDtos.asDto(agg.getTitle()).forEach(t -> builder.addTitle(t));

    return builder.build();
  }

  @NotNull
  Mica.OpalCredentialDto asDto(@NotNull OpalCredential credential) {
    Mica.OpalCredentialDto.Builder builder = Mica.OpalCredentialDto.newBuilder().setType(
      credential.getAuthType() == AuthType.USERNAME
        ? Mica.OpalCredentialType.USERNAME
        : Mica.OpalCredentialType.PUBLIC_KEY_CERTIFICATE).setOpalUrl(credential.getOpalUrl());

    if(!Strings.isNullOrEmpty(credential.getUsername())) builder.setUsername(credential.getUsername());

    return builder.build();
  }

  @NotNull
  Mica.DataAccessFormDto asDto(@NotNull DataAccessForm dataAccessForm) {
    Mica.DataAccessFormDto.Builder builder = Mica.DataAccessFormDto.newBuilder() //
      .setDefinition(dataAccessForm.getDefinition()) //
      .setSchema(dataAccessForm.getSchema()) //
      .addAllPdfTemplates(
        dataAccessForm.getPdfTemplates().values().stream().map(p -> attachmentDtos.asDto(p)).collect(toList())) //
      .addAllProperties(asDtoList(dataAccessForm.getProperties()));

    if(dataAccessForm.hasTitleFieldPath()) {
      builder.setTitleFieldPath(dataAccessForm.getTitleFieldPath());
    }

    if(dataAccessForm.hasIdPrefix()) {
      builder.setIdPrefix(dataAccessForm.getIdPrefix());
    }
    builder.setIdLength(dataAccessForm.getIdLength()) //
      .setNotifySubmitted(dataAccessForm.isNotifySubmitted()) //
      .setNotifyReviewed(dataAccessForm.isNotifyReviewed()) //
      .setNotifyApproved(dataAccessForm.isNotifyApproved()) //
      .setNotifyRejected(dataAccessForm.isNotifyRejected()) //
      .setNotifyReopened(dataAccessForm.isNotifyReopened()) //
      .setNotifyCommented(dataAccessForm.isNotifyCommented()) //
      .setWithReview(dataAccessForm.isWithReview()) //
      .setApprovedFinal(dataAccessForm.isApprovedFinal()) //
      .setRejectedFinal(dataAccessForm.isRejectedFinal());

    if(dataAccessForm.getSubmittedSubject() != null) builder.setSubmittedSubject(dataAccessForm.getSubmittedSubject());

    if(dataAccessForm.getReviewedSubject() != null) builder.setReviewedSubject(dataAccessForm.getReviewedSubject());

    if(dataAccessForm.getApprovedSubject() != null) builder.setApprovedSubject(dataAccessForm.getApprovedSubject());

    if(dataAccessForm.getRejectedSubject() != null) builder.setRejectedSubject(dataAccessForm.getRejectedSubject());

    if(dataAccessForm.getReopenedSubject() != null) builder.setReopenedSubject(dataAccessForm.getReopenedSubject());

    if(dataAccessForm.getCommentedSubject() != null) builder.setCommentedSubject(dataAccessForm.getCommentedSubject());

    return builder.build();
  }

  @NotNull
  DataAccessForm fromDto(@NotNull Mica.DataAccessFormDto dto) {
    DataAccessForm dataAccessForm = new DataAccessForm();
    dataAccessForm.setSchema(dto.getSchema());
    dataAccessForm.setDefinition(dto.getDefinition());

    dataAccessForm.setProperties(dto.getPropertiesList().stream()
      .collect(toMap(e -> e.getName(), e -> localizedStringDtos.fromDto(e.getValueList()))));

    dataAccessForm.setPdfTemplates(
      dto.getPdfTemplatesList().stream().map(t -> attachmentDtos.fromDto(t)).collect(toMap(a -> a.getLang(), x -> x)));

    if(dto.hasTitleFieldPath()) {
      dataAccessForm.setTitleFieldPath(dto.getTitleFieldPath());
    }

    if(dto.hasIdPrefix()) {
      dataAccessForm.setIdPrefix(dto.getIdPrefix());
    }
    dataAccessForm.setIdLength(dto.getIdLength());

    dataAccessForm.setNotifySubmitted(dto.getNotifySubmitted());
    dataAccessForm.setSubmittedSubject(dto.getSubmittedSubject());

    dataAccessForm.setNotifyReviewed(dto.getNotifyReviewed());
    dataAccessForm.setReviewedSubject(dto.getReviewedSubject());

    dataAccessForm.setNotifyApproved(dto.getNotifyApproved());
    dataAccessForm.setApprovedSubject(dto.getApprovedSubject());

    dataAccessForm.setNotifyRejected(dto.getNotifyRejected());
    dataAccessForm.setRejectedSubject(dto.getRejectedSubject());

    dataAccessForm.setNotifyReopened(dto.getNotifyReopened());
    dataAccessForm.setReopenedSubject(dto.getReopenedSubject());

    dataAccessForm.setNotifyCommented(dto.getNotifyCommented());
    dataAccessForm.setCommentedSubject(dto.getCommentedSubject());

    dataAccessForm.setWithReview(dto.getWithReview());
    dataAccessForm.setApprovedFinal(dto.getApprovedFinal());
    dataAccessForm.setRejectedFinal(dto.getRejectedFinal());

    return dataAccessForm;
  }

  @NotNull
  List<Mica.DataAccessFormDto.LocalizedPropertyDto> asDtoList(@NotNull Map<String, LocalizedString> properties) {
    return properties.entrySet().stream().map(
      e -> Mica.DataAccessFormDto.LocalizedPropertyDto.newBuilder().setName(e.getKey())
        .addAllValue(localizedStringDtos.asDto(e.getValue())).build()).collect(toList());
  }
}
