package org.obiba.mica.web.rest.dto;

import java.util.List;

import com.google.common.base.Objects;

public class UserDTO {

  private String login;

  private String firstName;

  private String lastName;

  private String email;

  private List<String> roles;

  public UserDTO() {
  }

  public UserDTO(String login, String firstName, String lastName, String email, List<String> roles) {
    this.login = login;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.roles = roles;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("login", login).add("firstName", firstName).add("lastName", lastName)
        .add("email", email).add("roles", roles).toString();
  }
}
