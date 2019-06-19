/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
import com.google.common.collect.Maps;

import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.PdfDownloadType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataAccessForm extends AbstractDataAccessEntityForm {

  private Map<Locale, Attachment> pdfTemplates;

  private PdfDownloadType pdfDownloadType = PdfDownloadType.Template;

  private String idPrefix;

  private int idLength = 6;

  private boolean allowIdWithLeadingZeros = true;

  private boolean notifySubmitted = true;

  private boolean notifyReviewed = true;

  private boolean notifyConditionallyApproved = true;

  private boolean notifyApproved = true;

  private boolean notifyRejected = true;

  private boolean notifyReopened = true;

  private boolean notifyCommented = true;

  private boolean notifyAttachment = true;

  private String submittedSubject;

  private String reviewedSubject;

  private String conditionallyApprovedSubject;

  private String approvedSubject;

  private String rejectedSubject;

  private String reopenedSubject;

  private String commentedSubject;

  private String attachmentSubject;

  private boolean withReview = true;

  private boolean withConditionalApproval = false;

  private boolean approvedFinal = false;

  private boolean rejectedFinal = false;

  private List<String> predefinedActions = null;

  private boolean amendmentsEnabled = false;

  private boolean daoCanEdit = false;

  public DataAccessForm() {
    super();
  }

  public Map<Locale, Attachment> getPdfTemplates() {
    return pdfTemplates == null ? pdfTemplates = Maps.newHashMap() : pdfTemplates;
  }

  public void setPdfTemplates(Map<Locale, Attachment> pdfTemplates) {
    this.pdfTemplates = pdfTemplates;
  }

  public PdfDownloadType getPdfDownloadType() {
    return pdfDownloadType;
  }

  public void setPdfDownloadType(PdfDownloadType pdfDownloadType) {
    this.pdfDownloadType = pdfDownloadType;
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

  public boolean isAmendmentsEnabled() {
    return amendmentsEnabled;
  }

  public void setAmendmentsEnabled(boolean amendmentsEnabled) {
    this.amendmentsEnabled = amendmentsEnabled;
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
}
