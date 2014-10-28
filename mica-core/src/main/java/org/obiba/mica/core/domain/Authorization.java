package org.obiba.mica.core.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Authorization implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  private boolean authorized;

  private String authorizer;

  private Date date;

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

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @JsonIgnore
  public void setDate(LocalDate date) {
    this.date = new Date();
    this.date.setTime(date.toEpochDay());
  }

  public LocalDate asLocalDate() {
    return LocalDate.ofEpochDay(getDate().getTime());
  }
}
