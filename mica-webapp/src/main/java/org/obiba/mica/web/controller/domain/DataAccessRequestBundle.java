package org.obiba.mica.web.controller.domain;

import java.time.LocalDateTime;

import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;

import javax.xml.crypto.Data;

public class DataAccessRequestBundle {

  private final String id;
  private final DataAccessRequest request;

  private final DataAccessPreliminary preliminary;
  private final String title;
  private final int totalAmendments;
  private final int pendingAmendments;
  private final int totalFeasibilities;
  private final int pendingFeasibilities;
  private StatusChange submission;

  public DataAccessRequestBundle(DataAccessRequest request, DataAccessPreliminary preliminary,
                                 String title, int totalAmendments, int pendingAmendments, int totalFeasibilities, int pendingFeasibilities) {
    this.id = request.getId();
    this.request = request;
    this.preliminary = preliminary;
    this.title = title;
    this.submission = request.getLastSubmission();
    this.totalAmendments = totalAmendments;
    this.pendingAmendments = pendingAmendments;
    this.totalFeasibilities = totalFeasibilities;
    this.pendingFeasibilities = pendingFeasibilities;
  }

  public String getId() {
    return id;
  }

  public String getApplicant() {
    return request.getApplicant();
  }

  public DataAccessRequest getRequest() {
    return request;
  }

  public DataAccessPreliminary getPreliminary() {
    return preliminary;
  }

  public String getTitle() {
    return title;
  }

  public LocalDateTime getLastUpdate() {
    return request.getLastModifiedDate().orElse(LocalDateTime.now());
  }

  public LocalDateTime getSubmitDate() {
    return submission == null ? null : submission.getChangedOn();
  }

  public DataAccessEntityStatus getStatus() {
    return request.getStatus();
  }

  public int getTotalAmendments() {
    return totalAmendments;
  }

  public int getPendingAmendments() {
    return pendingAmendments;
  }

  public int getTotalFeasibilities() {
    return totalFeasibilities;
  }

  public int getPendingFeasibilities() {
    return pendingFeasibilities;
  }

  public boolean isArchived() {
    return request.isArchived();
  }
}
