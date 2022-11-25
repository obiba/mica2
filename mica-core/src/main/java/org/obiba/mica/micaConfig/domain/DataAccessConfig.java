/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.domain;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.obiba.mica.core.domain.AbstractAuditableDocument;

import java.util.List;

public class DataAccessConfig extends AbstractAuditableDocument {

  final static int DEFAULT_ID_LENGTH = 6;

  private String idPrefix;

  private int idLength = 6;

  private boolean allowIdWithLeadingZeros = true;

  private boolean notifyCreated = false;

  private boolean notifySubmitted = true;

  private boolean notifyReviewed = true;

  private boolean notifyConditionallyApproved = true;

  private boolean notifyApproved = true;

  private boolean notifyRejected = true;

  private boolean notifyReopened = true;

  private boolean notifyCommented = true;

  private boolean notifyAttachment = true;

  private boolean notifyFinalReport = false;

  private boolean notifyIntermediateReport = false;

  private String createdSubject;

  private String submittedSubject;

  private String reviewedSubject;

  private String conditionallyApprovedSubject;

  private String approvedSubject;

  private String rejectedSubject;

  private String reopenedSubject;

  private String commentedSubject;

  private String attachmentSubject;

  private String finalReportSubject;

  private String intermediateReportSubject;

  private String collaboratorInvitationSubject;

  private boolean notifyCollaboratorAccepted = true;

  private String collaboratorAcceptedSubject;

  private boolean withReview = true;

  private boolean withConditionalApproval = false;

  private boolean approvedFinal = false;

  private boolean rejectedFinal = false;

  private List<String> predefinedActions = null;

  private boolean preliminaryEnabled = false;

  private boolean mergePreliminaryContentEnabled = false;

  private boolean feasibilityEnabled = false;

  private boolean agreementEnabled = false;

  private AgreementOpenedPolicy agreementOpenedPolicy = AgreementOpenedPolicy.ALWAYS;

  private boolean amendmentsEnabled = false;

  private boolean collaboratorsEnabled = true;

  private int collaboratorInvitationDays = 7;

  private boolean variablesEnabled = true;

  private boolean preliminaryVariablesEnabled = false;

  private boolean feasibilityVariablesEnabled = false;

  private boolean amendmentVariablesEnabled = false;

  private boolean daoCanEdit = false;

  private int nbOfDaysBeforeReport = 7;

  private String csvExportFormat;

  private String preliminaryCsvExportFormat;

  private String feasibilityCsvExportFormat;

  private String amendmentCsvExportFormat;

  public DataAccessConfig() {
    super();
  }

  public boolean hasIdPrefix() {
    return !Strings.isNullOrEmpty(idPrefix);
  }

  public String getIdPrefix() {
    return idPrefix;
  }

  public void setIdPrefix(String idPrefix) {
    this.idPrefix = idPrefix;
  }

  public int getIdLength() {
    return idLength;
  }

  public void setIdLength(int idLength) {
    this.idLength = idLength < DEFAULT_ID_LENGTH ? DEFAULT_ID_LENGTH : idLength;
  }

  public void setNotifyCreated(boolean notifyCreated) {
    this.notifyCreated = notifyCreated;
  }

  public boolean isNotifyCreated() {
    return notifyCreated;
  }

  public void setNotifySubmitted(boolean notifySubmitted) {
    this.notifySubmitted = notifySubmitted;
  }

  public boolean isNotifySubmitted() {
    return notifySubmitted;
  }

  public void setNotifyReviewed(boolean notifyReviewed) {
    this.notifyReviewed = notifyReviewed;
  }

  public boolean isNotifyReviewed() {
    return notifyReviewed;
  }

  public boolean isNotifyConditionallyApproved() {
    return notifyConditionallyApproved;
  }

  public void setNotifyConditionallyApproved(boolean notifyConditionallyApproved) {
    this.notifyConditionallyApproved = notifyConditionallyApproved;
  }

  public void setNotifyApproved(boolean notifyApproved) {
    this.notifyApproved = notifyApproved;
  }

  public boolean isNotifyApproved() {
    return notifyApproved;
  }

  public void setNotifyRejected(boolean notifyRejected) {
    this.notifyRejected = notifyRejected;
  }

  public boolean isNotifyRejected() {
    return notifyRejected;
  }

  public void setNotifyReopened(boolean notifyReopened) {
    this.notifyReopened = notifyReopened;
  }

  public boolean isNotifyReopened() {
    return notifyReopened;
  }

  public void setNotifyCommented(boolean notifyCommented) {
    this.notifyCommented = notifyCommented;
  }

  public boolean isNotifyCommented() {
    return notifyCommented;
  }

  public void setNotifyAttachment(boolean notifyAttachment) {
    this.notifyAttachment = notifyAttachment;
  }

  public boolean isNotifyAttachment() {
    return notifyAttachment;
  }

  public void setWithReview(boolean withReview) {
    this.withReview = withReview;
  }

  public boolean isWithReview() {
    return withReview;
  }

  public boolean isWithConditionalApproval() {
    return withConditionalApproval;
  }

  public void setWithConditionalApproval(boolean withConditionalApproval) {
    this.withConditionalApproval = withConditionalApproval;
  }

  public void setApprovedFinal(boolean approvedFinal) {
    this.approvedFinal = approvedFinal;
  }

  public boolean isApprovedFinal() {
    return approvedFinal;
  }

  public void setRejectedFinal(boolean rejectedFinal) {
    this.rejectedFinal = rejectedFinal;
  }

  public boolean isRejectedFinal() {
    return rejectedFinal;
  }

  public String getCreatedSubject() {
    return createdSubject;
  }

  public void setCreatedSubject(String createdSubject) {
    this.createdSubject = createdSubject;
  }

  public String getSubmittedSubject() {
    return submittedSubject;
  }

  public void setSubmittedSubject(String submittedSubject) {
    this.submittedSubject = submittedSubject;
  }

  public String getReviewedSubject() {
    return reviewedSubject;
  }

  public void setReviewedSubject(String reviewedSubject) {
    this.reviewedSubject = reviewedSubject;
  }

  public String getConditionallyApprovedSubject() {
    return conditionallyApprovedSubject;
  }

  public void setConditionallyApprovedSubject(String conditionallyApprovedSubject) {
    this.conditionallyApprovedSubject = conditionallyApprovedSubject;
  }

  public String getApprovedSubject() {
    return approvedSubject;
  }

  public void setApprovedSubject(String approvedSubject) {
    this.approvedSubject = approvedSubject;
  }

  public String getRejectedSubject() {
    return rejectedSubject;
  }

  public void setRejectedSubject(String rejectedSubject) {
    this.rejectedSubject = rejectedSubject;
  }

  public String getReopenedSubject() {
    return reopenedSubject;
  }

  public void setReopenedSubject(String reopenedSubject) {
    this.reopenedSubject = reopenedSubject;
  }

  public String getCommentedSubject() {
    return commentedSubject;
  }

  public void setCommentedSubject(String commentedSubject) {
    this.commentedSubject = commentedSubject;
  }

  public String getAttachmentSubject() {
    return attachmentSubject;
  }

  public void setAttachmentSubject(String attachmentSubject) {
    this.attachmentSubject = attachmentSubject;
  }

  public List<String> getPredefinedActions() {
    return predefinedActions == null ? Lists.newArrayList() : predefinedActions;
  }

  public void setPredefinedActions(List<String> predefinedActions) {
    this.predefinedActions = predefinedActions;
  }

  public boolean isPreliminaryEnabled() {
    return preliminaryEnabled;
  }

  public void setPreliminaryEnabled(boolean preliminaryEnabled) {
    this.preliminaryEnabled = preliminaryEnabled;
  }

  public boolean isMergePreliminaryContentEnabled() {
    return mergePreliminaryContentEnabled;
  }

  public void setMergePreliminaryContentEnabled(boolean mergePreliminaryContentEnabled) {
    this.mergePreliminaryContentEnabled = mergePreliminaryContentEnabled;
  }

  public boolean isFeasibilityEnabled() {
    return feasibilityEnabled;
  }

  public void setFeasibilityEnabled(boolean feasibilityEnabled) {
    this.feasibilityEnabled = feasibilityEnabled;
  }

  public boolean isAgreementEnabled() {
    return agreementEnabled;
  }

  public void setAgreementEnabled(boolean agreementEnabled) {
    this.agreementEnabled = agreementEnabled;
  }

  public AgreementOpenedPolicy getAgreementOpenedPolicy() {
    return agreementOpenedPolicy;
  }

  public void setAgreementOpenedPolicy(AgreementOpenedPolicy agreementOpenedPolicy) {
    this.agreementOpenedPolicy = agreementOpenedPolicy;
  }

  public boolean isAmendmentsEnabled() {
    return amendmentsEnabled;
  }

  public void setAmendmentsEnabled(boolean amendmentsEnabled) {
    this.amendmentsEnabled = amendmentsEnabled;
  }

  public boolean isCollaboratorsEnabled() {
    return collaboratorsEnabled;
  }

  public void setCollaboratorsEnabled(boolean collaboratorsEnabled) {
    this.collaboratorsEnabled = collaboratorsEnabled;
  }

  public int getCollaboratorInvitationDays() {
    return collaboratorInvitationDays;
  }

  public void setCollaboratorInvitationDays(int collaboratorInvitationDays) {
    this.collaboratorInvitationDays = collaboratorInvitationDays;
  }

  public void setVariablesEnabled(boolean variablesEnabled) {
    this.variablesEnabled = variablesEnabled;
  }

  public boolean isVariablesEnabled() {
    return variablesEnabled;
  }

  public void setPreliminaryVariablesEnabled(boolean preliminaryVariablesEnabled) {
    this.preliminaryVariablesEnabled = preliminaryVariablesEnabled;
  }

  public boolean isPreliminaryVariablesEnabled() {
    return preliminaryVariablesEnabled;
  }

  public void setFeasibilityVariablesEnabled(boolean feasibilityVariablesEnabled) {
    this.feasibilityVariablesEnabled = feasibilityVariablesEnabled;
  }

  public boolean isFeasibilityVariablesEnabled() {
    return feasibilityVariablesEnabled;
  }

  public void setAmendmentVariablesEnabled(boolean amendmentVariablesEnabled) {
    this.amendmentVariablesEnabled = amendmentVariablesEnabled;
  }

  public boolean isAmendmentVariablesEnabled() {
    return amendmentVariablesEnabled;
  }

  public boolean isDaoCanEdit() {
    return daoCanEdit;
  }

  public void setDaoCanEdit(boolean daoCanEdit) {
    this.daoCanEdit = daoCanEdit;
  }

  public boolean isAllowIdWithLeadingZeros() {
    return allowIdWithLeadingZeros;
  }

  public void setAllowIdWithLeadingZeros(boolean allowIdWithLeadingZeros) {
    this.allowIdWithLeadingZeros = allowIdWithLeadingZeros;
  }

  public void setNbOfDaysBeforeReport(int nbOfDaysBeforeReport) {
    this.nbOfDaysBeforeReport = nbOfDaysBeforeReport;
  }

  public int getNbOfDaysBeforeReport() {
    return nbOfDaysBeforeReport;
  }

  public void setNotifyFinalReport(boolean notifyFinalReport) {
    this.notifyFinalReport = notifyFinalReport;
  }

  public boolean isNotifyFinalReport() {
    return notifyFinalReport;
  }

  public String getFinalReportSubject() {
    return finalReportSubject;
  }

  public void setFinalReportSubject(String finalReportSubject) {
    this.finalReportSubject = finalReportSubject;
  }

  public void setNotifyIntermediateReport(boolean notifyIntermediateReport) {
    this.notifyIntermediateReport = notifyIntermediateReport;
  }

  public boolean isNotifyIntermediateReport() {
    return notifyIntermediateReport;
  }

  public void setIntermediateReportSubject(String intermediateReportSubject) {
    this.intermediateReportSubject = intermediateReportSubject;
  }

  public String getIntermediateReportSubject() {
    return intermediateReportSubject;
  }

  public String getCsvExportFormat() {
    return csvExportFormat;
  }

  public void setCsvExportFormat(String csvExportFormat) {
    this.csvExportFormat = csvExportFormat;
  }

  public String getPreliminaryCsvExportFormat() {
    return preliminaryCsvExportFormat;
  }

  public void setPreliminaryCsvExportFormat(String preliminaryCsvExportFormat) {
    this.preliminaryCsvExportFormat = preliminaryCsvExportFormat;
  }

  public String getFeasibilityCsvExportFormat() {
    return feasibilityCsvExportFormat;
  }

  public void setFeasibilityCsvExportFormat(String feasibilityCsvExportFormat) {
    this.feasibilityCsvExportFormat = feasibilityCsvExportFormat;
  }

  public String getAmendmentCsvExportFormat() {
    return amendmentCsvExportFormat;
  }

  public void setAmendmentCsvExportFormat(String amendmentCsvExportFormat) {
    this.amendmentCsvExportFormat = amendmentCsvExportFormat;
  }

  public String getCollaboratorInvitationSubject() {
    return collaboratorInvitationSubject;
  }

  public void setCollaboratorInvitationSubject(String collaboratorInvitationSubject) {
    this.collaboratorInvitationSubject = collaboratorInvitationSubject;
  }

  public boolean isNotifyCollaboratorAccepted() {
    return notifyCollaboratorAccepted;
  }

  public void setNotifyCollaboratorAccepted(boolean notifyCollaboratorAccepted) {
    this.notifyCollaboratorAccepted = notifyCollaboratorAccepted;
  }

  public String getCollaboratorAcceptedSubject() {
    return collaboratorAcceptedSubject;
  }

  public void setCollaboratorAcceptedSubject(String collaboratorAcceptedSubject) {
    this.collaboratorAcceptedSubject = collaboratorAcceptedSubject;
  }
}
