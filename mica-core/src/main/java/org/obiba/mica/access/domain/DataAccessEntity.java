package org.obiba.mica.access.domain;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.SchemaFormContentAware;
import org.springframework.data.mongodb.core.mapping.DBRef;

import javax.validation.constraints.NotNull;
import java.util.List;

public abstract class DataAccessEntity extends AbstractAuditableDocument implements SchemaFormContentAware {

  /**
   * User name of the user making the request.
   */
  @NotNull
  protected String applicant;

  /**
   * Json string containing the request data.
   */
  protected String content;

  protected DataAccessEntityStatus status = DataAccessEntityStatus.OPENED;

  private List<StatusChange> statusChangeHistory;

  private List<ActionLog> actionLogHistory;

  @DBRef
  private DocumentSet variablesSet;

  private Integer formRevision;

  //
  // Accessors
  //

  public String getApplicant() {
    return applicant;
  }

  public void setApplicant(String applicant) {
    this.applicant = applicant;
  }

  public DataAccessEntityStatus getStatus() {
    return status;
  }

  public void setStatus(DataAccessEntityStatus status) {
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

  public DateTime getSubmissionDate() {
    for (int i = getStatusChangeHistory().size()-1; i>=0; i--) {
      StatusChange chg = getStatusChangeHistory().get(i);
      if (chg.getTo().equals(DataAccessEntityStatus.SUBMITTED)) {
        return chg.getChangedOn();
      }
    }
    return null;
  }

  public boolean hasStatusChangeHistory() {
    return statusChangeHistory != null && !statusChangeHistory.isEmpty();
  }

  public List<StatusChange> getStatusChangeHistory() {
    if (statusChangeHistory == null) statusChangeHistory = Lists.newArrayList();
    return statusChangeHistory;
  }

  public void setStatusChangeHistory(List<StatusChange> statusChangeHistory) {
    this.statusChangeHistory = statusChangeHistory;
  }

  public boolean hasActionLogHistory() {
    return actionLogHistory != null && !actionLogHistory.isEmpty();
  }

  public List<ActionLog> getActionLogHistory() {
    if (actionLogHistory == null) actionLogHistory = Lists.newArrayList();
    return actionLogHistory;
  }

  public void setActionLogHistory(List<ActionLog> actionLogHistory) {
    this.actionLogHistory = actionLogHistory;
  }

  public DocumentSet getVariablesSet() {
    return variablesSet;
  }

  public boolean hasVariablesSet() {
    return variablesSet != null;
  }

  public void setVariablesSet(DocumentSet variablesSet) {
    this.variablesSet = variablesSet;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public void setFormRevision(Integer formRevision) {
    this.formRevision = formRevision;
  }

  public Integer getFormRevision() {
    return formRevision;
  }

  public boolean hasFormRevision() {
    return formRevision != null;
  }

  public static class Builder {
    protected DataAccessEntity request;

    public Builder applicant(String applicant) {
      request.applicant = applicant;
      return this;
    }

    public Builder status(String status) {
      request.status = DataAccessEntityStatus.valueOf(status.toUpperCase());
      return this;
    }

    public Builder content(String content) {
      request.content = content;
      return this;
    }

    public DataAccessEntity build() {
      return request;
    }
  }

}
