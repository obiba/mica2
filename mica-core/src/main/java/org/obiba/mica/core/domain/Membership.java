package org.obiba.mica.core.domain;

import java.io.Serializable;

import org.springframework.data.mongodb.core.mapping.DBRef;

public class Membership implements Serializable {

  public final static String INVESTIGATOR = "investigator";

  public final static String CONTACT = "contact";

  @DBRef
  private Person person;

  private String role;

  public Membership() {

  }

  public Membership(Person person, String role) {
    this.person = person;
    this.role = role;
  }

  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
