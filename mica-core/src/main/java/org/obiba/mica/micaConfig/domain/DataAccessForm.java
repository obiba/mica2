package org.obiba.mica.micaConfig.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.PdfDownloadType;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public class DataAccessForm extends AbstractGitPersistable {

  public final static String DEFAULT_ID = "default";

  public final static int DEFAULT_ID_LENGTH = 6;

  private String schema;

  private String definition;

  private String csvExportFormat;

  private Map<Locale, Attachment> pdfTemplates;

  private PdfDownloadType pdfDownloadType = PdfDownloadType.Template;

  private Map<String, LocalizedString> properties;

  private String titleFieldPath;

  private String summaryFieldPath;

  private String idPrefix;

  private int idLength = 6;

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

  @Indexed
  private RevisionStatus revisionStatus = RevisionStatus.DRAFT;

  @Indexed
  private String publishedTag;

  private int revisionsAhead = 0;

  public DataAccessForm() {
    setId(DEFAULT_ID);
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public String getCsvExportFormat() {
    return csvExportFormat;
  }

  public void setCsvExportFormat(String csvExportFormat) {
    this.csvExportFormat = csvExportFormat;
  }

  public Map<String, LocalizedString> getProperties() {
    return properties == null ? properties = Maps.newHashMap() : properties;
  }

  public void setProperties(Map<String, LocalizedString> properties) {
    this.properties = properties;
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

  @JsonIgnore
  public RevisionStatus getRevisionStatus() {
    return revisionStatus;
  }

  public void setRevisionStatus(RevisionStatus revisionStatus) {
    this.revisionStatus = revisionStatus;
  }

  @JsonIgnore
  public String getPublishedTag() {
    return publishedTag;
  }

  public void setPublishedTag(String publishedTag) {
    this.publishedTag = publishedTag;
  }

  @JsonIgnore
  public int getRevisionsAhead() {
    return revisionsAhead;
  }

  public void setRevisionsAhead(int revisionsAhead) {
    this.revisionsAhead = revisionsAhead;
  }

  public void incrementRevisionsAhead() {
    revisionsAhead++;
  }

  @Override
  public String pathPrefix() {
    return "data-access-forms";
  }

  @Override
  public Map<String, Serializable> parts() {
    final DataAccessForm self = this;

    return new HashMap<String, Serializable>(){
      {
        put(self.getClass().getSimpleName(), self);
        put("definition", self.getDefinition());
        put("schema", self.getSchema());
      }
    };
  }

  public boolean hasTitleFieldPath() {
    return !Strings.isNullOrEmpty(titleFieldPath);
  }

  public String getTitleFieldPath() {
    return titleFieldPath;
  }

  public void setTitleFieldPath(String titleFieldPath) {
    this.titleFieldPath = titleFieldPath;
  }

  public boolean hasSummaryFieldPath() {
    return !Strings.isNullOrEmpty(summaryFieldPath);
  }

  public String getSummaryFieldPath() {
    return summaryFieldPath;
  }

  public void setSummaryFieldPath(String summaryFieldPath) {
    this.summaryFieldPath = summaryFieldPath;
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
}
