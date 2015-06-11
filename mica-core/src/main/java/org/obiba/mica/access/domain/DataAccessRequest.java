package org.obiba.mica.access.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.PersistableWithAttachments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 *
 */
public class DataAccessRequest extends AbstractAuditableDocument
  implements AttachmentAware, PersistableWithAttachments {

  private static final long serialVersionUID = -6728220507676973832L;

  /**
   * User name of the user making the request.
   */
  @NotNull
  private String applicant;

  @JsonIgnore
  private String title;

  /**
   * Json string containing the request data.
   */
  private String content;

  private Status status = Status.OPENED;

  private List<Attachment> attachments;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    checkStatusTransition(status);
    this.status = status;
  }

  public boolean hasContent() {
    return !Strings.isNullOrEmpty(content);
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  //
  // Attachments
  //

  @Override
  @NotNull
  public List<Attachment> getAttachments() {
    if(attachments == null) attachments = new ArrayList<>();
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
    this.attachments = attachments;
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

  @Override
  public String pathPrefix() {
    return null;
  }

  @Override
  public Map<String, Serializable> parts() {
    return null;
  }

  //
  // Helpers
  //

  /**
   * Get the possible next status.
   *
   * @return
   */
  @JsonIgnore
  public Iterable<Status> nextStatus() {
    List<Status> to = Lists.newArrayList();
    switch(status) {
      case OPENED:
        to.add(Status.SUBMITTED);
        break;
      case SUBMITTED:
        to.add(Status.OPENED);
        to.add(Status.REVIEWED);
        break;
      case REVIEWED:
        to.add(Status.OPENED);
        to.add(Status.APPROVED);
        to.add(Status.REJECTED);
        break;
      case APPROVED:
      case REJECTED:
        // final state
        break;
    }
    return to;
  }

  /**
   * Check if a status transition is valid.
   *
   * @param to
   * @throws IllegalArgumentException
   */
  @SuppressWarnings("OverlyLongMethod")
  @JsonIgnore
  private void checkStatusTransition(Status to) throws IllegalArgumentException {
    if(status == to) return;

    switch(status) {
      case OPENED:
        if(to != Status.SUBMITTED)
          throw new IllegalArgumentException("Opened data access request can only be submitted");
        break;
      case SUBMITTED:
        if(to != Status.OPENED && to != Status.REVIEWED)
          throw new IllegalArgumentException("Submitted data access request can only be reopened or put under review");
        break;
      case REVIEWED:
        if(to != Status.OPENED && to != Status.APPROVED &&
          to != Status.REJECTED) throw new IllegalArgumentException(
          "Reviewed data access request can only be reopened or be approved/rejected");
        break;
      case APPROVED:
        throw new IllegalArgumentException("Approved data access request cannot be modified");
      case REJECTED:
        throw new IllegalArgumentException("Rejected data access request cannot be modified");
      default:
        throw new IllegalArgumentException("Unexpected data access request status: " + status);
    }
  }

  //
  // Inner classes and enums
  //

  public enum Status {
    OPENED,     // request is being edited by the applicant
    SUBMITTED, // request is submitted by the applicant, ready for review
    REVIEWED,  // request is being reviewed
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

    public Builder title(String title) {
      request.title = title;
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
