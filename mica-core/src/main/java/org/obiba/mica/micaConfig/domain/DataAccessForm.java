package org.obiba.mica.micaConfig.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.PersistableWithAttachments;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class DataAccessForm extends AbstractGitPersistable implements PersistableWithAttachments {

  public final static String DEFAULT_ID = "default";

  public final static int DEFAULT_ID_LENGTH = 6;

  private String schema;

  private String definition;

  private Map<Locale, Attachment> pdfTemplates;

  private Map<String, LocalizedString> properties;

  private String titleFieldPath;

  private String idPrefix;

  private int idLength = 6;

  private boolean notifySubmitted = true;

  private boolean notifyReviewed = true;

  private boolean notifyApproved = true;

  private boolean notifyRejected = true;

  private boolean notifyReopened = true;

  private boolean notifyCommented = true;

  private boolean withReview = true;

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

  public Map<String, LocalizedString> getProperties() {
    return properties == null ? properties = Maps.newHashMap() : properties;
  }

  public void setProperties(Map<String, LocalizedString> properties) {
    this.properties = properties;
  }

  @JsonIgnore
  @Override
  public Iterable<Attachment> getAllAttachments() {
    return Iterables.concat(getPdfTemplates().values());
  }

  @Override
  public Attachment findAttachmentById(String attachmentId) {
    return null;
  }

  public Map<Locale, Attachment> getPdfTemplates() {
    return pdfTemplates == null ? pdfTemplates = Maps.newHashMap() : pdfTemplates;
  }

  public void setPdfTemplates(Map<Locale, Attachment> pdfTemplates) {
    this.pdfTemplates = pdfTemplates;
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

  public void setWithReview(boolean withReview) {
    this.withReview = withReview;
  }

  public boolean isWithReview() {
    return withReview;
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
}
