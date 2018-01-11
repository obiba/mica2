/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.domain;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.core.domain.SchemaFormContentAware;
import org.obiba.mica.file.Attachment;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
@Document
public class DataAccessRequest extends AbstractAuditableDocument implements AttachmentAware, SchemaFormContentAware {

  private static final long serialVersionUID = -6728220507676973832L;

  /**
   * User name of the user making the request.
   */
  @NotNull
  private String applicant;

  /**
   * Json string containing the request data.
   */
  private String content;

  private Status status = Status.OPENED;

  @DBRef
  private List<Attachment> attachments = Lists.newArrayList();

  private Iterable<Attachment> removedAttachments = Lists.newArrayList();

  private List<StatusChange> statusChangeHistory;

  //
  // Accessors
  //

  public String getApplicant() {
    return applicant;
  }

  public void setApplicant(String applicant) {
    this.applicant = applicant;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public boolean hasContent() {
    return !Strings.isNullOrEmpty(content);
  }

  @Override
  public String getContent() {
    return content;
  }

  @Override
  public void setContent(String content) {
    this.content = content;
  }

  //
  // Attachments
  //

  @Override
  @NotNull
  public List<Attachment> getAttachments() {
    return attachments;
  }

  @Override
  public boolean hasAttachments() {
    return attachments != null && !attachments.isEmpty();
  }

  @Override
  public void addAttachment(@NotNull Attachment attachment) {
    getAttachments().add(attachment);
  }

  @Override
  public void setAttachments(List<Attachment> attachments) {
    if(attachments == null) attachments = Lists.newArrayList();

    this.removedAttachments = Sets.difference(Sets.newHashSet(this.attachments), Sets.newHashSet(attachments));
    this.attachments = attachments;
  }

  @Override
  public List<Attachment> removedAttachments() {
    return Lists.newArrayList(removedAttachments);
  }

  @JsonIgnore
  @Override
  public Iterable<Attachment> getAllAttachments() {
    return () -> getAttachments().stream().filter(a -> a != null).iterator();
  }

  @Override
  public Attachment findAttachmentById(String attachmentId) {
    return getAttachments().stream().filter(a -> a != null && a.getId().equals(attachmentId)).findAny().orElse(null);
  }

  public boolean hasStatusChangeHistory() {
    return statusChangeHistory != null && !statusChangeHistory.isEmpty();
  }

  public List<StatusChange> getStatusChangeHistory() {
    if (statusChangeHistory == null) statusChangeHistory = Lists.newArrayList();
    return statusChangeHistory;
  }

  public void addAttachment(@NotNull StatusChange statusChange) {
    getStatusChangeHistory().add(statusChange);
  }

  public void setStatusChangeHistory(List<StatusChange> statusChangeHistory) {
    this.statusChangeHistory = statusChangeHistory;
  }

  //
  // Inner classes and enums
  //

  public enum Status {
    OPENED,     // request is being edited by the applicant
    SUBMITTED, // request is submitted by the applicant, ready for review
    REVIEWED,  // request is being reviewed
    CONDITIONALLY_APPROVED,
    APPROVED,  // request was reviewed and approved
    REJECTED   // request was reviewed and rejected
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private DataAccessRequest request;

    public Builder() {
      request = new DataAccessRequest();
    }

    public Builder applicant(String applicant) {
      request.applicant = applicant;
      return this;
    }

    public Builder status(String status) {
      request.status = Status.valueOf(status.toUpperCase());
      return this;
    }

    public Builder content(String content) {
      request.content = content;
      return this;
    }

    public DataAccessRequest build() {
      return request;
    }
  }

}
