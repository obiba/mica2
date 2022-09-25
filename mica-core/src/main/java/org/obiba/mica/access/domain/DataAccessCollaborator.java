package org.obiba.mica.access.domain;

import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.AbstractAuditableDocument;

import javax.validation.constraints.NotNull;

public class DataAccessCollaborator extends AbstractAuditableDocument {

  private static final long serialVersionUID = 1890344645568884322L;

  @NotNull
  private String requestId;

  private String email;

  private String principal;

  private boolean invitationPending;

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  public boolean isInvitationPending() {
    return invitationPending;
  }

  public void setInvitationPending(boolean invitationPending) {
    this.invitationPending = invitationPending;
  }

  public static DataAccessCollaborator.Builder newBuilder(String requestId) {
    return new DataAccessCollaborator.Builder(requestId);
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public String getPrincipal() {
    return principal;
  }

  public boolean hasPrincipal() {
    return !Strings.isNullOrEmpty(principal);
  }

  public static class Builder {
    DataAccessCollaborator collaborator;

    public Builder(String requestId) {
      this.collaborator = new DataAccessCollaborator();
      this.collaborator.requestId = requestId;
      this.collaborator.setCreatedDate(DateTime.now());
    }

    public Builder email(String email) {
      this.collaborator.email = email;
      return this;
    }

    public Builder invited() {
      this.collaborator.invitationPending = true;
      return this;
    }

    public Builder author(String author) {
      this.collaborator.setCreatedBy(author);
      return this;
    }

    public DataAccessCollaborator build() {
      return collaborator;
    }

  }
}
