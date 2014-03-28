package org.obiba.mica.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Authorization implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  private boolean authorized;

  private String authorizer;

  private LocalDateTime date;

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

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }
}
