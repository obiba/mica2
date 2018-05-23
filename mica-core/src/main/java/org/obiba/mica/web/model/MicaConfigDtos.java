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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.AuthType;
import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.PdfDownloadType;
import org.obiba.mica.micaConfig.domain.*;
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
  Mica.PublicMicaConfigDto asPublicDto(@NotNull MicaConfig config) {
    Mica.PublicMicaConfigDto.Builder builder = Mica.PublicMicaConfigDto.newBuilder() //
      .setName(config.getName()) //
      .setOpenAccess(config.isOpenAccess());
    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));

    if (config.hasPublicUrl()) {
      builder.setPublicUrl(config.getPublicUrl());
    }

    builder.addAllAvailableLayoutOptions(Arrays.asList(MicaConfig.LAYOUT_OPTIONS));
    builder.setSearchLayout(config.getSearchLayout());

    return builder.build();
  }

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
    if(!Strings.isNullOrEmpty(config.getPortalUrl())) {
      builder.setPortalUrl(config.getPortalUrl());
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

    builder.setIsCollectedDatasetNotificationsEnabled(config.isStudyDatasetNotificationsEnabled());
    if(config.getStudyDatasetNotificationsSubject() != null) builder.setCollectedDatasetNotificationsSubject(config.getStudyDatasetNotificationsSubject());

    builder.setIsHarmonizedDatasetNotificationsEnabled(config.isHarmonizationDatasetNotificationsEnabled());
    if(config.getHarmonizationDatasetNotificationsSubject() != null) builder.setHarmonizedDatasetNotificationsSubject(config.getHarmonizationDatasetNotificationsSubject());

    builder.setIsProjectNotificationsEnabled(config.isProjectNotificationsEnabled());
    if(config.getProjectNotificationsSubject() != null) builder.setProjectNotificationsSubject(config.getProjectNotificationsSubject());

    builder.setIsSingleNetworkEnabled(config.isSingleNetworkEnabled());
    builder.setIsSingleStudyEnabled(config.isSingleStudyEnabled());
    builder.setIsNetworkEnabled(config.isNetworkEnabled());
    builder.setIsCollectedDatasetEnabled(config.isStudyDatasetEnabled());
    builder.setIsHarmonizedDatasetEnabled(config.isHarmonizationDatasetEnabled());

    if(config.hasStyle()) builder.setStyle(config.getStyle());

    if(config.hasTranslations()) builder.addAllTranslations(localizedStringDtos.asDto(config.getTranslations()));

    builder.addAllAvailableLayoutOptions(Arrays.asList(MicaConfig.LAYOUT_OPTIONS));
    builder.setSearchLayout(config.getSearchLayout());

    return builder.build();
  }

  @NotNull
  MicaConfig fromDto(@NotNull Mica.MicaConfigDtoOrBuilder dto) {
    MicaConfig config = new MicaConfig();
    config.setName(dto.getName());
    config.setDefaultCharacterSet(dto.getDefaultCharSet());
    config.setOpenAccess(dto.getOpenAccess());

    if (dto.hasSearchLayout()) config.setSearchLayout(dto.getSearchLayout());

    if(dto.hasPublicUrl()) config.setPublicUrl(dto.getPublicUrl());
    if(dto.hasPortalUrl()) config.setPortalUrl(dto.getPortalUrl());

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
    config.setStudyDatasetNotificationsEnabled(dto.getIsCollectedDatasetNotificationsEnabled());
    if(dto.hasCollectedDatasetNotificationsSubject()) config.setStudyDatasetNotificationsSubject(dto.getCollectedDatasetNotificationsSubject());
    config.setHarmonizationDatasetNotificationsEnabled(dto.getIsHarmonizedDatasetNotificationsEnabled());
    if(dto.hasHarmonizedDatasetNotificationsSubject()) config.setHarmonizationDatasetNotificationsSubject(dto.getHarmonizedDatasetNotificationsSubject());
    config.setProjectNotificationsEnabled(dto.getIsProjectNotificationsEnabled());
    if(dto.hasProjectNotificationsSubject()) config.setProjectNotificationsSubject(dto.getProjectNotificationsSubject());

    config.setSingleNetworkEnabled(dto.getIsSingleNetworkEnabled());
    config.setSingleStudyEnabled(dto.getIsSingleStudyEnabled());
    config.setNetworkEnabled(dto.getIsNetworkEnabled());
    config.setStudyDatasetEnabled(dto.getIsCollectedDatasetEnabled());
    config.setHarmonizationDatasetEnabled(dto.getIsHarmonizedDatasetEnabled());

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
      .setNotifyAttachment(dataAccessForm.isNotifyAttachment()) //
      .setWithReview(dataAccessForm.isWithReview()) //
      .setApprovedFinal(dataAccessForm.isApprovedFinal()) //
      .setRejectedFinal(dataAccessForm.isRejectedFinal())
      .setWithConditionalApproval(dataAccessForm.isWithConditionalApproval())
      .setNotifyConditionallyApproved(dataAccessForm.isNotifyConditionallyApproved())
      .setCsvExportFormat(dataAccessForm.getCsvExportFormat());

    if(dataAccessForm.getSubmittedSubject() != null) builder.setSubmittedSubject(dataAccessForm.getSubmittedSubject());

    if(dataAccessForm.getReviewedSubject() != null) builder.setReviewedSubject(dataAccessForm.getReviewedSubject());

    if(dataAccessForm.getApprovedSubject() != null) builder.setApprovedSubject(dataAccessForm.getApprovedSubject());

    if(dataAccessForm.getRejectedSubject() != null) builder.setRejectedSubject(dataAccessForm.getRejectedSubject());

    if(dataAccessForm.getReopenedSubject() != null) builder.setReopenedSubject(dataAccessForm.getReopenedSubject());

    if(dataAccessForm.getCommentedSubject() != null) builder.setCommentedSubject(dataAccessForm.getCommentedSubject());

    if(dataAccessForm.getAttachmentSubject() != null) builder.setAttachmentSubject(dataAccessForm.getAttachmentSubject());

    if(dataAccessForm.getConditionallyApprovedSubject() != null) builder.setConditionallyApprovedSubject(dataAccessForm.getConditionallyApprovedSubject());

    if(dataAccessForm.getPredefinedActions() != null) builder.addAllPredefinedActions(dataAccessForm.getPredefinedActions());

    builder.setPdfDownloadType(Mica.DataAccessFormDto.PdfDownloadType.valueOf(dataAccessForm.getPdfDownloadType().name()));
    return builder.build();
  }

  @NotNull
  DataAccessForm fromDto(@NotNull Mica.DataAccessFormDto dto) {
    DataAccessForm dataAccessForm = new DataAccessForm();
    dataAccessForm.setSchema(dto.getSchema());
    dataAccessForm.setDefinition(dto.getDefinition());
    dataAccessForm.setCsvExportFormat(dto.getCsvExportFormat());

    dataAccessForm.setProperties(dto.getPropertiesList().stream()
      .collect(toMap(Mica.LocalizedPropertyDto::getName, e -> localizedStringDtos.fromDto(e.getValueList()))));

    dataAccessForm.setPdfTemplates(
      dto.getPdfTemplatesList().stream().map(t -> attachmentDtos.fromDto(t)).collect(toMap(Attachment::getLang, x -> x)));

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

    dataAccessForm.setNotifyAttachment(dto.getNotifyAttachment());
    dataAccessForm.setAttachmentSubject(dto.getAttachmentSubject());

    dataAccessForm.setWithReview(dto.getWithReview());
    dataAccessForm.setApprovedFinal(dto.getApprovedFinal());
    dataAccessForm.setRejectedFinal(dto.getRejectedFinal());

    dataAccessForm.setWithConditionalApproval(dto.getWithConditionalApproval());
    dataAccessForm.setNotifyConditionallyApproved(dto.getNotifyConditionallyApproved());
    dataAccessForm.setConditionallyApprovedSubject(dto.getConditionallyApprovedSubject());
    dataAccessForm.setPdfDownloadType(PdfDownloadType.valueOf(dto.getPdfDownloadType().name()));
    dataAccessForm.setPredefinedActions(dto.getPredefinedActionsList());

    return dataAccessForm;
  }

  Mica.DataAccessAmendmentFormDto asDto(@NotNull DataAccessAmendmentForm dataAccessAmendmentForm) {
    Mica.DataAccessAmendmentFormDto.Builder builder = Mica.DataAccessAmendmentFormDto.newBuilder()
      .setDefinition(dataAccessAmendmentForm.getDefinition()).setSchema(dataAccessAmendmentForm.getSchema())
      .addAllProperties(asDtoList(dataAccessAmendmentForm.getProperties()))
      .setCsvExportFormat(dataAccessAmendmentForm.getCsvExportFormat());

    if(dataAccessAmendmentForm.hasTitleFieldPath()) {
      builder.setTitleFieldPath(dataAccessAmendmentForm.getTitleFieldPath());
    }

    if(dataAccessAmendmentForm.hasSummaryFieldPath()) {
      builder.setSummaryFieldPath(dataAccessAmendmentForm.getSummaryFieldPath());
    }

    return builder.build();
  }

  DataAccessAmendmentForm fromDto(@NotNull Mica.DataAccessAmendmentFormDto dto) {
    DataAccessAmendmentForm dataAccessAmendmentForm = new DataAccessAmendmentForm();

    dataAccessAmendmentForm.setSchema(dto.getSchema());
    dataAccessAmendmentForm.setDefinition(dto.getDefinition());
    dataAccessAmendmentForm.setCsvExportFormat(dto.getCsvExportFormat());

    dataAccessAmendmentForm.setProperties(dto.getPropertiesList().stream()
      .collect(toMap(Mica.LocalizedPropertyDto::getName, e -> localizedStringDtos.fromDto(e.getValueList()))));

    if(dto.hasTitleFieldPath()) {
      dataAccessAmendmentForm.setTitleFieldPath(dto.getTitleFieldPath());
    }

    if(dto.hasSummaryFieldPath()) {
      dataAccessAmendmentForm.setSummaryFieldPath(dto.getSummaryFieldPath());
    }

    return dataAccessAmendmentForm;
  }

  @NotNull
  Mica.ProjectFormDto asDto(@NotNull ProjectConfig projectConfig) {
    Mica.ProjectFormDto.Builder builder = Mica.ProjectFormDto.newBuilder() //
      .setDefinition(projectConfig.getDefinition()) //
      .setSchema(projectConfig.getSchema()) //
      .addAllProperties(asDtoList(projectConfig.getProperties()));

    return builder.build();
  }

  @NotNull
  <T extends EntityConfig> T fromDto(@NotNull Mica.EntityFormDto dto) {

    EntityConfig config = null;

    switch(dto.getType()) {
      case Network:
        config = new NetworkConfig();
        break;
      case Study:
        config = new StudyConfig();
        break;
      case DataCollectionEvent:
        config = new DataCollectionEventConfig();
        break;
      case Population:
        config = new PopulationConfig();
        break;
      case CollectedDataset:
        config = new StudyDatasetConfig();
        break;
      case HarmonizedDataset:
        config = new HarmonizationDatasetConfig();
        break;
      case HarmonizationStudy:
        config = new HarmonizationStudyConfig();
        break;
      case HarmonizationPopulation:
        config = new HarmonizationPopulationConfig();
        break;
    }

    config.setSchema(dto.getSchema());
    config.setDefinition(dto.getDefinition());
    return (T)config;
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull NetworkConfig networkConfig) {
    return asDto(networkConfig, Mica.EntityFormDto.Type.Network);
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull DatasetConfig datasetConfig) {
    return asDto(datasetConfig, datasetConfig instanceof StudyDatasetConfig ?
        Mica.EntityFormDto.Type.CollectedDataset : Mica.EntityFormDto.Type.HarmonizedDataset);
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull StudyConfig studyConfig) {
    return asDto(studyConfig, Mica.EntityFormDto.Type.Study);
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull HarmonizationStudyConfig harmonizationStudyConfig) {
    return asDto(harmonizationStudyConfig, Mica.EntityFormDto.Type.HarmonizationStudy);
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull PopulationConfig populationConfig) {
    return asDto(populationConfig, Mica.EntityFormDto.Type.Population);
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull HarmonizationPopulationConfig harmonizationPopulationConfig) {
    return asDto(harmonizationPopulationConfig, Mica.EntityFormDto.Type.HarmonizationPopulation);
  }

  @NotNull
  Mica.EntityFormDto asDto(@NotNull DataCollectionEventConfig dataCollectionEventConfig) {
    return asDto(dataCollectionEventConfig, Mica.EntityFormDto.Type.DataCollectionEvent);
  }

  private Mica.EntityFormDto asDto(EntityConfig config, Mica.EntityFormDto.Type type) {
    Mica.EntityFormDto.Builder builder = Mica.EntityFormDto.newBuilder() //
      .setType(type)
      .setSchema(config.getSchema())
      .setDefinition(config.getDefinition());

    return builder.build();
  }

  @NotNull
  ProjectConfig fromDto(@NotNull Mica.ProjectFormDto dto) {
    ProjectConfig projectConfig = new ProjectConfig();
    projectConfig.setSchema(dto.getSchema());
    projectConfig.setDefinition(dto.getDefinition());

    return projectConfig;
  }

  @NotNull
  List<Mica.LocalizedPropertyDto> asDtoList(@NotNull Map<String, LocalizedString> properties) {
    return properties.entrySet().stream().map(
      e -> Mica.LocalizedPropertyDto.newBuilder().setName(e.getKey())
        .addAllValue(localizedStringDtos.asDto(e.getValue())).build()).collect(toList());
  }
}
