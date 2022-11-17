/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
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
import org.obiba.mica.core.service.AgateServerConfigService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.domain.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.PdfDownloadType;
import org.obiba.mica.micaConfig.domain.*;
import org.obiba.mica.micaConfig.domain.MicaConfig.OpalViewsGrouping;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
class MicaConfigDtos {

  private LocalizedStringDtos localizedStringDtos;

  private AttachmentDtos attachmentDtos;

  private SubjectAclService subjectAclService;

  private AgateServerConfigService agateServerConfigService;

  @Inject
  public MicaConfigDtos(
    LocalizedStringDtos localizedStringDtos,
    AttachmentDtos attachmentDtos,
    SubjectAclService subjectAclService,
    AgateServerConfigService agateServerConfigService) {
    this.localizedStringDtos = localizedStringDtos;
    this.attachmentDtos = attachmentDtos;
    this.subjectAclService = subjectAclService;
    this.agateServerConfigService = agateServerConfigService;
  }

  MicaConfigDtos() {
    this.localizedStringDtos = new LocalizedStringDtos();
  }

  @NotNull
  Mica.PublicMicaConfigDto asPublicDto(@NotNull MicaConfig config) {
    Mica.PublicMicaConfigDto.Builder builder = Mica.PublicMicaConfigDto.newBuilder()
      .setName(config.getName())
      .setOpenAccess(config.isOpenAccess())
      .setAgateUrl(agateServerConfigService.getAgateUrl());

    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));

    if (config.hasPublicUrl()) {
      builder.setPublicUrl(config.getPublicUrl());
    }

    builder.addAllAvailableLayoutOptions(Arrays.asList(MicaConfig.LAYOUT_OPTIONS));
    builder.setSearchLayout(config.getSearchLayout());

    builder.setMaxItemsPerSet(config.getMaxItemsPerSet());
    builder.setMaxNumberOfSets(config.getMaxNumberOfSets());

    return builder.build();
  }

  @NotNull
  Mica.MicaConfigDto asDto(@NotNull MicaConfig config) {
    return asDto(config, null);
  }

  @NotNull
  Mica.MicaConfigDto asDto(@NotNull MicaConfig config, String language) {
    Mica.MicaConfigDto.Builder builder = Mica.MicaConfigDto.newBuilder()
      .setName(config.getName())
      .setDefaultCharSet(config.getDefaultCharacterSet())//
      .setOpenAccess(config.isOpenAccess());
    config.getLocales().forEach(locale -> builder.addLanguages(locale.getLanguage()));

    if(!Strings.isNullOrEmpty(config.getPublicUrl())) {
      builder.setPublicUrl(config.getPublicUrl());
    }
    if(!Strings.isNullOrEmpty(config.getPortalUrl())) {
      builder.setPortalUrl(config.getPortalUrl());
    }
    builder.setIsUsePublicUrlForSharedLink(config.isUsePublicUrlForSharedLink());

    builder.setOpal(config.getOpal());
    builder.setPrivacyThreshold(config.getPrivacyThreshold());

    if(config.getMicaVersion() != null) {
      builder.setVersion(config.getMicaVersion().toString());
    }

    builder.setIsCommentsRequiredOnDocumentSave(config.isCommentsRequiredOnDocumentSave());

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

    builder.setIsContactNotificationsEnabled(config.isContactNotificationsEnabled());
    if(config.getContactNotificationsSubject() != null) builder.setContactNotificationsSubject(config.getContactNotificationsSubject());
    config.getContactGroups().forEach(builder::addContactGroups);

    builder.setIsRepositoryEnabled(config.isRepositoryEnabled());
    builder.setIsDataAccessEnabled(config.isDataAccessEnabled());
    builder.setIsProjectEnabled(config.isProjectEnabled());
    builder.setIsSingleNetworkEnabled(config.isSingleNetworkEnabled());
    builder.setIsSingleStudyEnabled(config.isSingleStudyEnabled());
    builder.setIsNetworkEnabled(config.isNetworkEnabled());
    builder.setIsCollectedDatasetEnabled(config.isStudyDatasetEnabled());
    builder.setIsHarmonizedDatasetEnabled(config.isHarmonizationDatasetEnabled());
    builder.setVariableSummaryRequiresAuthentication(config.isVariableSummaryRequiresAuthentication());
    builder.setIsImportStudiesFeatureEnabled(config.isImportStudiesFeatureEnabled());

    if(config.hasStyle()) builder.setStyle(config.getStyle());

    if(config.hasTranslations()) builder.addAllTranslations(localizedStringDtos.asDto(config.getTranslations(), language));

    builder.addAllAvailableLayoutOptions(Arrays.asList(MicaConfig.LAYOUT_OPTIONS));
    builder.setSearchLayout(config.getSearchLayout());

    builder.setMaxItemsPerSet(config.getMaxItemsPerSet());
    builder.setMaxNumberOfSets(config.getMaxNumberOfSets());
    builder.setIsCartEnabled(config.isCartEnabled());
    builder.setAnonymousCanCreateCart(config.isAnonymousCanCreateCart());
    builder.setIsStudiesCartEnabled(config.isStudiesCartEnabled());
    builder.setIsNetworksCartEnabled(config.isNetworksCartEnabled());
    builder.setSetTimeToLive(config.getSetTimeToLive());
    builder.setCartTimeToLive(config.getCartTimeToLive());
    builder.setIsStudiesCompareEnabled(config.isStudiesCompareEnabled());
    builder.setIsNetworksCompareEnabled(config.isNetworksCompareEnabled());
    builder.setMaxItemsPerCompare(config.getMaxItemsPerCompare());
    builder.setIsStudiesExportEnabled(config.isStudiesExportEnabled());
    builder.setIsNetworksExportEnabled(config.isNetworksExportEnabled());
    builder.setIsContingencyEnabled(config.isContingencyEnabled());
    builder.setIsSetsAnalysisEnabled(config.isSetsAnalysisEnabled());
    builder.setIsSetsSearchEnabled(config.isSetsSearchEnabled());
    builder.setOpalViewsGrouping(config.getOpalViewsGrouping().name());

    if (!subjectAclService.hasMicaRole()) {
      builder.setCurrentUserCanCreateCart(false);
      builder.setCurrentUserCanCreateSets(false);
    } else {
      builder.setCurrentUserCanCreateCart(config.isCartEnabled());
      builder.setCurrentUserCanCreateSets(true);
    }

    builder.setSignupEnabled(config.isSignupEnabled());
    builder.setSignupWithPassword(config.isSignupWithPassword());
    config.getSignupGroups().forEach(builder::addSignupGroups);

    builder.setDownloadOpalViewsFromSetsAllowed(subjectAclService.isPermitted("/set/documents", "VIEW", "_opal"));

    builder.setIsVariablesCountEnabled(config.isVariablesCountEnabled());
    builder.setIsProjectsCountEnabled(config.isProjectsCountEnabled());
    builder.setIsDataAccessRequestsCountEnabled(config.isDataAccessRequestCountEnabled());

    return builder.build();
  }

  @NotNull
  MicaConfig fromDto(@NotNull Mica.MicaConfigDtoOrBuilder dto) {
    MicaConfig config = new MicaConfig();
    config.setName(dto.getName());
    config.setDefaultCharacterSet(dto.getDefaultCharSet());
    config.setOpenAccess(dto.getOpenAccess());

    config.setOpalViewsGrouping(OpalViewsGrouping.valueOf(dto.getOpalViewsGrouping()));

    config.setCommentsRequiredOnDocumentSave(dto.getIsCommentsRequiredOnDocumentSave());

    if (dto.hasSearchLayout()) config.setSearchLayout(dto.getSearchLayout());

    if(dto.hasPublicUrl()) config.setPublicUrl(dto.getPublicUrl());
    if(dto.hasPortalUrl()) config.setPortalUrl(dto.getPortalUrl());
    if(dto.hasIsUsePublicUrlForSharedLink()) config.setUsePublicUrlForSharedLink(dto.getIsUsePublicUrlForSharedLink());

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
    config.setContactNotificationsEnabled(dto.getIsContactNotificationsEnabled());
    if(dto.hasContactNotificationsSubject()) config.setContactNotificationsSubject(dto.getContactNotificationsSubject());
    config.setContactGroups(dto.getContactGroupsList());

    if (dto.hasIsRepositoryEnabled()) config.setRepositoryEnabled(dto.getIsRepositoryEnabled());
    if (dto.hasIsDataAccessEnabled()) config.setDataAccessEnabled(dto.getIsDataAccessEnabled());
    if (dto.hasIsProjectEnabled()) config.setProjectEnabled(dto.getIsProjectEnabled());
    config.setSingleNetworkEnabled(dto.getIsSingleNetworkEnabled());
    config.setSingleStudyEnabled(dto.getIsSingleStudyEnabled());
    config.setNetworkEnabled(dto.getIsNetworkEnabled());
    config.setStudyDatasetEnabled(dto.getIsCollectedDatasetEnabled());
    config.setHarmonizationDatasetEnabled(dto.getIsHarmonizedDatasetEnabled());
    config.setImportStudiesFeatureEnabled(dto.getIsImportStudiesFeatureEnabled());
    if (dto.hasVariableSummaryRequiresAuthentication()) config.setVariableSummaryRequiresAuthentication(dto.getVariableSummaryRequiresAuthentication());

    if (dto.hasSignupEnabled()) config.setSignupEnabled(dto.getSignupEnabled());
    config.setSignupWithPassword(dto.getSignupWithPassword());
    config.setSignupGroups(dto.getSignupGroupsList());

    config.setCartEnabled(dto.getIsCartEnabled());
    config.setAnonymousCanCreateCart(dto.getAnonymousCanCreateCart());
    config.setStudiesCartEnabled(dto.getIsStudiesCartEnabled());
    config.setNetworksCartEnabled(dto.getIsNetworksCartEnabled());
    config.setStudiesCompareEnabled(dto.getIsStudiesCompareEnabled());
    config.setNetworksCompareEnabled(dto.getIsNetworksCompareEnabled());
    config.setMaxItemsPerCompare(dto.getMaxItemsPerCompare());
    config.setStudiesExportEnabled(dto.getIsStudiesExportEnabled());
    config.setNetworksExportEnabled(dto.getIsNetworksExportEnabled());
    config.setSetsAnalysisEnabled(dto.getIsSetsAnalysisEnabled());
    config.setContingencyEnabled(dto.getIsContingencyEnabled());
    config.setSetsSearchEnabled(dto.getIsSetsSearchEnabled());

    if (dto.hasMaxItemsPerSet() && dto.getMaxItemsPerSet() > 0) config.setMaxItemsPerSet(dto.getMaxItemsPerSet());
    if (dto.hasMaxNumberOfSets() && dto.getMaxNumberOfSets() > 0) config.setMaxNumberOfSets(dto.getMaxNumberOfSets());
    if (dto.hasCartTimeToLive()) config.setCartTimeToLive(dto.getCartTimeToLive());
    if (dto.hasSetTimeToLive()) config.setSetTimeToLive(dto.getSetTimeToLive());

    if(dto.hasStyle()) config.setStyle(dto.getStyle());

    if(dto.getTranslationsCount() > 0) config.setTranslations(localizedStringDtos.fromDto(dto.getTranslationsList()));

    config.setVariablesCountEnabled(dto.getIsVariablesCountEnabled());
    config.setProjectsCountEnabled(dto.getIsProjectsCountEnabled());
    config.setDataAccessRequestCountEnabled(dto.getIsDataAccessRequestsCountEnabled());

    return config;
  }

  @NotNull
  Mica.OpalCredentialDto asDto(@NotNull OpalCredential credential) {
    Mica.OpalCredentialDto.Builder builder = Mica.OpalCredentialDto.newBuilder()
      .setOpalUrl(credential.getOpalUrl());

    switch (credential.getAuthType()) {
      case USERNAME:
        builder.setType(Mica.OpalCredentialType.USERNAME);
        if(!Strings.isNullOrEmpty(credential.getUsername()))
          builder.setUsername(credential.getUsername());
        break;
      case CERTIFICATE:
        builder.setType(Mica.OpalCredentialType.PUBLIC_KEY_CERTIFICATE);
        break;
      case TOKEN:
        builder.setType(Mica.OpalCredentialType.TOKEN);
        break;
    }

    return builder.build();
  }

  @NotNull
  Mica.DataAccessConfigDto asDto(@NotNull DataAccessConfig dataAccessConfig) {
    Mica.DataAccessConfigDto.Builder builder = Mica.DataAccessConfigDto.newBuilder()
      .setDaoCanEdit(dataAccessConfig.isDaoCanEdit());

    if(dataAccessConfig.hasIdPrefix()) {
      builder.setIdPrefix(dataAccessConfig.getIdPrefix());
    }

    builder.setAllowIdWithLeadingZeros(dataAccessConfig.isAllowIdWithLeadingZeros());

    builder.setIdLength(dataAccessConfig.getIdLength())
      .setNotifyCreated(dataAccessConfig.isNotifyCreated())
      .setNotifySubmitted(dataAccessConfig.isNotifySubmitted())
      .setNotifyReviewed(dataAccessConfig.isNotifyReviewed())
      .setNotifyApproved(dataAccessConfig.isNotifyApproved())
      .setNotifyRejected(dataAccessConfig.isNotifyRejected())
      .setNotifyReopened(dataAccessConfig.isNotifyReopened())
      .setNotifyCommented(dataAccessConfig.isNotifyCommented())
      .setNotifyAttachment(dataAccessConfig.isNotifyAttachment())
      .setNotifyFinalReport(dataAccessConfig.isNotifyFinalReport())
      .setNotifyIntermediateReport(dataAccessConfig.isNotifyIntermediateReport())
      .setNotifyCollaboratorAccepted(dataAccessConfig.isNotifyCollaboratorAccepted())
      .setWithReview(dataAccessConfig.isWithReview())
      .setApprovedFinal(dataAccessConfig.isApprovedFinal())
      .setRejectedFinal(dataAccessConfig.isRejectedFinal())
      .setWithConditionalApproval(dataAccessConfig.isWithConditionalApproval())
      .setNotifyConditionallyApproved(dataAccessConfig.isNotifyConditionallyApproved())
      .setCsvExportFormat(dataAccessConfig.getCsvExportFormat())
      .setPreliminaryCsvExportFormat(dataAccessConfig.getPreliminaryCsvExportFormat())
      .setFeasibilityCsvExportFormat(dataAccessConfig.getFeasibilityCsvExportFormat())
      .setAmendmentCsvExportFormat(dataAccessConfig.getAmendmentCsvExportFormat());

    if(dataAccessConfig.getCreatedSubject() != null) builder.setCreatedSubject(dataAccessConfig.getCreatedSubject());

    if(dataAccessConfig.getSubmittedSubject() != null) builder.setSubmittedSubject(dataAccessConfig.getSubmittedSubject());

    if(dataAccessConfig.getReviewedSubject() != null) builder.setReviewedSubject(dataAccessConfig.getReviewedSubject());

    if(dataAccessConfig.getApprovedSubject() != null) builder.setApprovedSubject(dataAccessConfig.getApprovedSubject());

    if(dataAccessConfig.getRejectedSubject() != null) builder.setRejectedSubject(dataAccessConfig.getRejectedSubject());

    if(dataAccessConfig.getReopenedSubject() != null) builder.setReopenedSubject(dataAccessConfig.getReopenedSubject());

    if(dataAccessConfig.getCommentedSubject() != null) builder.setCommentedSubject(dataAccessConfig.getCommentedSubject());

    if(dataAccessConfig.getAttachmentSubject() != null) builder.setAttachmentSubject(dataAccessConfig.getAttachmentSubject());

    if(dataAccessConfig.getConditionallyApprovedSubject() != null) builder.setConditionallyApprovedSubject(dataAccessConfig.getConditionallyApprovedSubject());

    if(dataAccessConfig.getFinalReportSubject() != null) builder.setFinalReportSubject(dataAccessConfig.getFinalReportSubject());

    if(dataAccessConfig.getIntermediateReportSubject() != null) builder.setIntermediateReportSubject(dataAccessConfig.getIntermediateReportSubject());

    builder.setNbOfDaysBeforeReport(dataAccessConfig.getNbOfDaysBeforeReport());

    if(dataAccessConfig.getCollaboratorInvitationSubject() != null) builder.setCollaboratorInvitationSubject(dataAccessConfig.getCollaboratorInvitationSubject());
    if(dataAccessConfig.getCollaboratorAcceptedSubject() != null) builder.setCollaboratorAcceptedSubject(dataAccessConfig.getCollaboratorAcceptedSubject());

    if(dataAccessConfig.getPredefinedActions() != null) builder.addAllPredefinedActions(dataAccessConfig.getPredefinedActions());

    builder.setPreliminaryEnabled(dataAccessConfig.isPreliminaryEnabled());
    builder.setFeasibilityEnabled(dataAccessConfig.isFeasibilityEnabled());
    builder.setAgreementEnabled(dataAccessConfig.isAgreementEnabled());
    builder.setAmendmentsEnabled(dataAccessConfig.isAmendmentsEnabled());
    builder.setCollaboratorsEnabled(dataAccessConfig.isCollaboratorsEnabled());
    builder.setCollaboratorInvitationDays(dataAccessConfig.getCollaboratorInvitationDays());

    builder.setVariablesEnabled(dataAccessConfig.isVariablesEnabled());
    builder.setPreliminaryVariablesEnabled(dataAccessConfig.isPreliminaryVariablesEnabled());
    builder.setFeasibilityVariablesEnabled(dataAccessConfig.isFeasibilityVariablesEnabled());
    builder.setAmendmentVariablesEnabled(dataAccessConfig.isAmendmentVariablesEnabled());

    builder.setMergePreliminaryContentEnabled(dataAccessConfig.isMergePreliminaryContentEnabled());

    return builder.build();
  }

  @NotNull
  DataAccessConfig fromDto(@NotNull Mica.DataAccessConfigDto dto) {
    DataAccessConfig dataAccessConfig = new DataAccessConfig();
    dataAccessConfig.setDaoCanEdit(dto.getDaoCanEdit());

    if(dto.hasIdPrefix()) {
      dataAccessConfig.setIdPrefix(dto.getIdPrefix());
    }

    dataAccessConfig.setAllowIdWithLeadingZeros(dto.getAllowIdWithLeadingZeros());

    dataAccessConfig.setIdLength(dto.getIdLength());

    dataAccessConfig.setNotifyCreated(dto.getNotifyCreated());
    dataAccessConfig.setCreatedSubject(dto.getCreatedSubject());

    dataAccessConfig.setNotifySubmitted(dto.getNotifySubmitted());
    dataAccessConfig.setSubmittedSubject(dto.getSubmittedSubject());

    dataAccessConfig.setNotifyReviewed(dto.getNotifyReviewed());
    dataAccessConfig.setReviewedSubject(dto.getReviewedSubject());

    dataAccessConfig.setNotifyApproved(dto.getNotifyApproved());
    dataAccessConfig.setApprovedSubject(dto.getApprovedSubject());

    dataAccessConfig.setNotifyRejected(dto.getNotifyRejected());
    dataAccessConfig.setRejectedSubject(dto.getRejectedSubject());

    dataAccessConfig.setNotifyReopened(dto.getNotifyReopened());
    dataAccessConfig.setReopenedSubject(dto.getReopenedSubject());

    dataAccessConfig.setNotifyCommented(dto.getNotifyCommented());
    dataAccessConfig.setCommentedSubject(dto.getCommentedSubject());

    dataAccessConfig.setNotifyAttachment(dto.getNotifyAttachment());
    dataAccessConfig.setAttachmentSubject(dto.getAttachmentSubject());

    dataAccessConfig.setNotifyFinalReport(dto.getNotifyFinalReport());
    dataAccessConfig.setFinalReportSubject(dto.getFinalReportSubject());

    dataAccessConfig.setNotifyIntermediateReport(dto.getNotifyIntermediateReport());
    dataAccessConfig.setIntermediateReportSubject(dto.getIntermediateReportSubject());

    if (dto.hasNbOfDaysBeforeReport()) dataAccessConfig.setNbOfDaysBeforeReport(dto.getNbOfDaysBeforeReport());

    dataAccessConfig.setCollaboratorInvitationSubject(dto.getCollaboratorInvitationSubject());
    dataAccessConfig.setNotifyCollaboratorAccepted(dto.getNotifyCollaboratorAccepted());
    dataAccessConfig.setCollaboratorAcceptedSubject(dto.getCollaboratorAcceptedSubject());

    dataAccessConfig.setWithReview(dto.getWithReview());
    dataAccessConfig.setApprovedFinal(dto.getApprovedFinal());
    dataAccessConfig.setRejectedFinal(dto.getRejectedFinal());

    dataAccessConfig.setWithConditionalApproval(dto.getWithConditionalApproval());
    dataAccessConfig.setNotifyConditionallyApproved(dto.getNotifyConditionallyApproved());
    dataAccessConfig.setConditionallyApprovedSubject(dto.getConditionallyApprovedSubject());
    dataAccessConfig.setPredefinedActions(dto.getPredefinedActionsList());
    dataAccessConfig.setPreliminaryEnabled(dto.getPreliminaryEnabled());
    dataAccessConfig.setFeasibilityEnabled(dto.getFeasibilityEnabled());
    dataAccessConfig.setAgreementEnabled(dto.getAgreementEnabled());
    dataAccessConfig.setAmendmentsEnabled(dto.getAmendmentsEnabled());
    dataAccessConfig.setCollaboratorsEnabled(dto.getCollaboratorsEnabled());
    dataAccessConfig.setCollaboratorInvitationDays(dto.getCollaboratorInvitationDays());

    dataAccessConfig.setVariablesEnabled(dto.getVariablesEnabled());
    dataAccessConfig.setPreliminaryVariablesEnabled(dto.getPreliminaryVariablesEnabled());
    dataAccessConfig.setFeasibilityVariablesEnabled(dto.getFeasibilityVariablesEnabled());
    dataAccessConfig.setAmendmentVariablesEnabled(dto.getAmendmentVariablesEnabled());

    dataAccessConfig.setMergePreliminaryContentEnabled(dto.getMergePreliminaryContentEnabled());

    if (dto.hasCsvExportFormat()) dataAccessConfig.setCsvExportFormat(dto.getCsvExportFormat());
    if (dto.hasPreliminaryCsvExportFormat()) dataAccessConfig.setPreliminaryCsvExportFormat(dto.getPreliminaryCsvExportFormat());
    if (dto.hasFeasibilityCsvExportFormat()) dataAccessConfig.setFeasibilityCsvExportFormat(dto.getFeasibilityCsvExportFormat());
    if (dto.hasAmendmentCsvExportFormat()) dataAccessConfig.setAmendmentCsvExportFormat(dto.getAmendmentCsvExportFormat());

    return dataAccessConfig;
  }


  @NotNull
  Mica.DataAccessFormDto asDto(@NotNull DataAccessForm dataAccessForm) {
    Mica.DataAccessFormDto.Builder builder = Mica.DataAccessFormDto.newBuilder()
      .setRevision(dataAccessForm.getRevision())
      .setLastUpdateDate(dataAccessForm.getLastUpdateDate().toString())
      .setDefinition(dataAccessForm.getDefinition())
      .setSchema(dataAccessForm.getSchema())
      .addAllPdfTemplates(
        dataAccessForm.getPdfTemplates().values().stream().map(p -> attachmentDtos.asDto(p)).collect(toList()))
      .addAllProperties(asDtoList(dataAccessForm.getProperties()));

    if(dataAccessForm.hasTitleFieldPath()) {
      builder.setTitleFieldPath(dataAccessForm.getTitleFieldPath());
    }

    if(dataAccessForm.hasSummaryFieldPath()) {
      builder.setSummaryFieldPath(dataAccessForm.getSummaryFieldPath());
    }

    if(dataAccessForm.hasEndDateFieldPath()) {
      builder.setEndDateFieldPath(dataAccessForm.getEndDateFieldPath());
    }

    builder.setPdfDownloadType(Mica.DataAccessFormDto.PdfDownloadType.valueOf(dataAccessForm.getPdfDownloadType().name()));

    return builder.build();
  }

  @NotNull
  DataAccessForm fromDto(@NotNull Mica.DataAccessFormDto dto) {
    DataAccessForm dataAccessForm = new DataAccessForm();
    dataAccessForm.setSchema(dto.getSchema());
    dataAccessForm.setDefinition(dto.getDefinition());

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

    if(dto.hasEndDateFieldPath()) {
      dataAccessForm.setEndDateFieldPath(dto.getEndDateFieldPath());
    }
    dataAccessForm.setPdfDownloadType(PdfDownloadType.valueOf(dto.getPdfDownloadType().name()));

    return dataAccessForm;
  }

  Mica.DataAccessPreliminaryFormDto asDto(@NotNull DataAccessPreliminaryForm form) {
    Mica.DataAccessPreliminaryFormDto.Builder builder = Mica.DataAccessPreliminaryFormDto.newBuilder()
      .setRevision(form.getRevision())
      .setLastUpdateDate(form.getLastUpdateDate().toString())
      .setDefinition(form.getDefinition())
      .setSchema(form.getSchema());
    return builder.build();
  }

  DataAccessPreliminaryForm fromDto(@NotNull Mica.DataAccessPreliminaryFormDto dto) {
    DataAccessPreliminaryForm form = new DataAccessPreliminaryForm();

    form.setSchema(dto.getSchema());
    form.setDefinition(dto.getDefinition());

    return form;
  }

  Mica.DataAccessFeasibilityFormDto asDto(@NotNull DataAccessFeasibilityForm form) {
    Mica.DataAccessFeasibilityFormDto.Builder builder = Mica.DataAccessFeasibilityFormDto.newBuilder()
      .setRevision(form.getRevision())
      .setLastUpdateDate(form.getLastUpdateDate().toString())
      .setDefinition(form.getDefinition())
      .setSchema(form.getSchema());
    return builder.build();
  }

  DataAccessFeasibilityForm fromDto(@NotNull Mica.DataAccessFeasibilityFormDto dto) {
    DataAccessFeasibilityForm form = new DataAccessFeasibilityForm();

    form.setSchema(dto.getSchema());
    form.setDefinition(dto.getDefinition());

    return form;
  }

  Mica.DataAccessAgreementFormDto asDto(@NotNull DataAccessAgreementForm form) {
    Mica.DataAccessAgreementFormDto.Builder builder = Mica.DataAccessAgreementFormDto.newBuilder()
      .setRevision(form.getRevision())
      .setLastUpdateDate(form.getLastUpdateDate().toString())
      .setDefinition(form.getDefinition())
      .setSchema(form.getSchema());
    return builder.build();
  }

  DataAccessAgreementForm fromDto(@NotNull Mica.DataAccessAgreementFormDto dto) {
    DataAccessAgreementForm form = new DataAccessAgreementForm();

    form.setSchema(dto.getSchema());
    form.setDefinition(dto.getDefinition());

    return form;
  }

  Mica.DataAccessAmendmentFormDto asDto(@NotNull DataAccessAmendmentForm form,
                                        @NotNull DataAccessConfig dataAccessConfig) {
    Mica.DataAccessAmendmentFormDto.Builder builder = Mica.DataAccessAmendmentFormDto.newBuilder()
      .setRevision(form.getRevision())
      .setLastUpdateDate(form.getLastUpdateDate().toString())
      .setDefinition(form.getDefinition())
      .setSchema(form.getSchema())
      .addAllProperties(asDtoList(form.getProperties()));

    if(form.hasTitleFieldPath()) {
      builder.setTitleFieldPath(form.getTitleFieldPath());
    }

    if(form.hasSummaryFieldPath()) {
      builder.setSummaryFieldPath(form.getSummaryFieldPath());
    }

    if(form.hasEndDateFieldPath()) {
      builder.setEndDateFieldPath(form.getEndDateFieldPath());
    }

    builder.setWithReview(dataAccessConfig.isWithReview());
    builder.setApprovedFinal(dataAccessConfig.isApprovedFinal());

    return builder.build();
  }

  DataAccessAmendmentForm fromDto(@NotNull Mica.DataAccessAmendmentFormDto dto) {
    DataAccessAmendmentForm form = new DataAccessAmendmentForm();

    form.setSchema(dto.getSchema());
    form.setDefinition(dto.getDefinition());

    form.setProperties(dto.getPropertiesList().stream()
      .collect(toMap(Mica.LocalizedPropertyDto::getName, e -> localizedStringDtos.fromDto(e.getValueList()))));

    if(dto.hasTitleFieldPath()) {
      form.setTitleFieldPath(dto.getTitleFieldPath());
    }

    if(dto.hasSummaryFieldPath()) {
      form.setSummaryFieldPath(dto.getSummaryFieldPath());
    }

    if(dto.hasEndDateFieldPath()) {
      form.setEndDateFieldPath(dto.getEndDateFieldPath());
    }

    return form;
  }

  @NotNull
  Mica.ProjectFormDto asDto(@NotNull ProjectConfig projectConfig) {
    Mica.ProjectFormDto.Builder builder = Mica.ProjectFormDto.newBuilder()
      .setDefinition(projectConfig.getDefinition())
      .setSchema(projectConfig.getSchema())
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
  Mica.EntityFormDto asDto(@NotNull DataCollectionEventConfig dataCollectionEventConfig) {
    return asDto(dataCollectionEventConfig, Mica.EntityFormDto.Type.DataCollectionEvent);
  }

  private Mica.EntityFormDto asDto(EntityConfig config, Mica.EntityFormDto.Type type) {
    Mica.EntityFormDto.Builder builder = Mica.EntityFormDto.newBuilder()
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
