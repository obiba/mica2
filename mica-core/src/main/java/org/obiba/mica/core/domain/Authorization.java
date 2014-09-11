package org.obiba.mica.core.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class Authorization implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  private boolean authorized;

  private String authorizer;

  private LocalDate date;

  public boolean isAuthorized() {
    return authorized;
  }

  public void setAuthorized(boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizer() {
    return authorizer;
  }

  public void setAuthorizer(String authorizer) {
    this.authorizer = authorizer;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }
}
