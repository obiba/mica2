package org.obiba.mica.web.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.domain.MicaConfig;
import org.obiba.mica.micaConfig.domain.OpalCredential;
import org.obiba.mica.micaConfig.domain.ProjectForm;
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
      .setDefaultCharSet(config.getDefaultCharacterSet())//
      .setOpenAccess(config.isOpenAccess());
    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));

    if(!Strings.isNullOrEmpty(config.getPublicUrl())) {
      builder.setPublicUrl(config.getPublicUrl());
    }

    builder.setOpal(config.getOpal());
    builder.setPrivacyThreshold(config.getPrivacyThreshold());

    if(config.getMicaVersion() != null) {
      builder.setVersion(config.getMicaVersion().toString());
    }

    builder.addAllRoles(config.getRoles());

    builder.setIsFsNotificationsEnabled(config.isFsNotificationsEnabled());
    if(config.getFsNotificationsSubject() != null) builder.setFsNotificationsSubject(config.getFsNotificationsSubject());

    builder.setIsCommentNotificationsEnabled(config.isCommentNotificationsEnabled());
    if(config.getCommentNotificationsSubject() != null) builder.setCommentNotificationsSubject(config.getCommentNotificationsSubject());

    builder.setIsNetworkNotificationsEnabled(config.isNetworkNotificationsEnabled());
    if(config.getNetworkNotificationsSubject() != null) builder.setNetworkNotificationsSubject(config.getNetworkNotificationsSubject());

    builder.setIsStudyNotificationsEnabled(config.isStudyNotificationsEnabled());
    if(config.getStudyNotificationsSubject() != null) builder.setStudyNotificationsSubject(config.getStudyNotificationsSubject());

    builder.setIsStudyDatasetNotificationsEnabled(config.isStudyDatasetNotificationsEnabled());
    if(config.getStudyDatasetNotificationsSubject() != null) builder.setStudyDatasetNotificationsSubject(config.getStudyDatasetNotificationsSubject());

    builder.setIsHarmonizationDatasetNotificationsEnabled(config.isHarmonizationDatasetNotificationsEnabled());
    if(config.getHarmonizationDatasetNotificationsSubject() != null) builder.setHarmonizationDatasetNotificationsSubject(config.getHarmonizationDatasetNotificationsSubject());

    builder.setIsProjectNotificationsEnabled(config.isProjectNotificationsEnabled());
    if(config.getProjectNotificationsSubject() != null) builder.setProjectNotificationsSubject(config.getProjectNotificationsSubject());

    builder.setIsSingleNetworkEnabled(config.isSingleNetworkEnabled());
    builder.setIsSingleStudyEnabled(config.isSingleStudyEnabled());
    builder.setIsNetworkEnabled(config.isNetworkEnabled());
    builder.setIsStudyDatasetEnabled(config.isStudyDatasetEnabled());
    builder.setIsHarmonizationDatasetEnabled(config.isHarmonizationDatasetEnabled());

    if(config.hasStyle()) builder.setStyle(config.getStyle());

    if(config.hasTranslations()) builder.addAllTranslations(localizedStringDtos.asDto(config.getTranslations()));

    return builder.build();
  }

  @NotNull
  MicaConfig fromDto(@NotNull Mica.MicaConfigDtoOrBuilder dto) {
    MicaConfig config = new MicaConfig();
    config.setName(dto.getName());
    config.setDefaultCharacterSet(dto.getDefaultCharSet());
    config.setOpenAccess(dto.getOpenAccess());

    if(dto.hasPublicUrl()) config.setPublicUrl(dto.getPublicUrl());

    dto.getLanguagesList().forEach(lang -> config.getLocales().add(new Locale(lang)));
    config.setOpal(dto.getOpal());
    if (dto.hasPrivacyThreshold()) config.setPrivacyThreshold(dto.getPrivacyThreshold());

    config.setRoles(dto.getRolesList());
    config.setFsNotificationsEnabled(dto.getIsFsNotificationsEnabled());
    if(dto.hasFsNotificationsSubject()) config.setFsNotificationsSubject(dto.getFsNotificationsSubject());
    config.setCommentNotificationsEnabled(dto.getIsCommentNotificationsEnabled());
    if(dto.hasCommentNotificationsSubject()) config.setCommentNotificationsSubject(dto.getCommentNotificationsSubject());
    config.setStudyNotificationsEnabled(dto.getIsStudyNotificationsEnabled());
    if(dto.hasStudyNotificationsSubject()) config.setStudyNotificationsSubject(dto.getStudyNotificationsSubject());
    config.setNetworkNotificationsEnabled(dto.getIsNetworkNotificationsEnabled());
    if(dto.hasNetworkNotificationsSubject()) config.setNetworkNotificationsSubject(dto.getNetworkNotificationsSubject());
    config.setStudyDatasetNotificationsEnabled(dto.getIsStudyDatasetNotificationsEnabled());
    if(dto.hasStudyDatasetNotificationsSubject()) config.setStudyDatasetNotificationsSubject(dto.getStudyDatasetNotificationsSubject());
    config.setHarmonizationDatasetNotificationsEnabled(dto.getIsHarmonizationDatasetNotificationsEnabled());
    if(dto.hasHarmonizationDatasetNotificationsSubject()) config.setHarmonizationDatasetNotificationsSubject(dto.getHarmonizationDatasetNotificationsSubject());
    config.setProjectNotificationsEnabled(dto.getIsProjectNotificationsEnabled());
    if(dto.hasProjectNotificationsSubject()) config.setProjectNotificationsSubject(dto.getProjectNotificationsSubject());

    config.setSingleNetworkEnabled(dto.getIsSingleNetworkEnabled());
    config.setSingleStudyEnabled(dto.getIsSingleStudyEnabled());
    config.setNetworkEnabled(dto.getIsNetworkEnabled());
    config.setStudyDatasetEnabled(dto.getIsStudyDatasetEnabled());
    config.setHarmonizationDatasetEnabled(dto.getIsHarmonizationDatasetEnabled());

    if(dto.hasStyle()) config.setStyle(dto.getStyle());

    if(dto.getTranslationsCount() > 0) config.setTranslations(localizedStringDtos.fromDto(dto.getTranslationsList()));

    return config;
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

    if(dataAccessForm.hasSummaryFieldPath()) {
      builder.setSummaryFieldPath(dataAccessForm.getSummaryFieldPath());
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
      .setRejectedFinal(dataAccessForm.isRejectedFinal())
      .setWithConditionalApproval(dataAccessForm.isWithConditionalApproval())
      .setNotifyConditionallyApproved(dataAccessForm.isNotifyConditionallyApproved());

    if(dataAccessForm.getSubmittedSubject() != null) builder.setSubmittedSubject(dataAccessForm.getSubmittedSubject());

    if(dataAccessForm.getReviewedSubject() != null) builder.setReviewedSubject(dataAccessForm.getReviewedSubject());

    if(dataAccessForm.getApprovedSubject() != null) builder.setApprovedSubject(dataAccessForm.getApprovedSubject());

    if(dataAccessForm.getRejectedSubject() != null) builder.setRejectedSubject(dataAccessForm.getRejectedSubject());

    if(dataAccessForm.getReopenedSubject() != null) builder.setReopenedSubject(dataAccessForm.getReopenedSubject());

    if(dataAccessForm.getCommentedSubject() != null) builder.setCommentedSubject(dataAccessForm.getCommentedSubject());

    if(dataAccessForm.getConditionallyApprovedSubject() != null) builder.setConditionallyApprovedSubject(dataAccessForm.getConditionallyApprovedSubject());

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

    if(dto.hasSummaryFieldPath()) {
      dataAccessForm.setSummaryFieldPath(dto.getSummaryFieldPath());
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

    dataAccessForm.setWithConditionalApproval(dto.getWithConditionalApproval());
    dataAccessForm.setNotifyConditionallyApproved(dto.getNotifyConditionallyApproved());
    dataAccessForm.setConditionallyApprovedSubject(dto.getConditionallyApprovedSubject());

    return dataAccessForm;
  }


  @NotNull
  Mica.ProjectFormDto asDto(@NotNull ProjectForm projectForm) {
    Mica.ProjectFormDto.Builder builder = Mica.ProjectFormDto.newBuilder() //
      .setDefinition(projectForm.getDefinition()) //
      .setSchema(projectForm.getSchema()) //
      .addAllProperties(asDtoList(projectForm.getProperties()));

    return builder.build();
  }

  @NotNull
  ProjectForm fromDto(@NotNull Mica.ProjectFormDto dto) {
    ProjectForm projectForm = new ProjectForm();
    projectForm.setSchema(dto.getSchema());
    projectForm.setDefinition(dto.getDefinition());

    projectForm.setProperties(dto.getPropertiesList().stream()
      .collect(toMap(e -> e.getName(), e -> localizedStringDtos.fromDto(e.getValueList()))));

    return projectForm;
  }

  @NotNull
  List<Mica.LocalizedPropertyDto> asDtoList(@NotNull Map<String, LocalizedString> properties) {
    return properties.entrySet().stream().map(
      e -> Mica.LocalizedPropertyDto.newBuilder().setName(e.getKey())
        .addAllValue(localizedStringDtos.asDto(e.getValue())).build()).collect(toList());
  }
}
