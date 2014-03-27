package org.obiba.mica.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

public class Contact implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  @NotBlank
  private String name;

  @Email
  private String email;

  private String phone;

  private boolean dataAccessCommitteeMember;

  private Institution institution;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isDataAccessCommitteeMember() {
    return dataAccessCommitteeMember;
  }

  public void setDataAccessCommitteeMember(boolean dataAccessCommitteeMember) {
    this.dataAccessCommitteeMember = dataAccessCommitteeMember;
  }

  public Institution getInstitution() {
    return institution;
  }

  public void setInstitution(Institution institution) {
    this.institution = institution;
  }

  public static class Institution implements Serializable {

    private static final long serialVersionUID = -3098622168836970902L;

    @NotNull
    private LocalizableString name;

    private LocalizableString department;

    private Address address;

    public LocalizableString getName() {
      return name;
    }

    public void setName(LocalizableString name) {
      this.name = name;
    }

    public LocalizableString getDepartment() {
      return department;
    }

    public void setDepartment(LocalizableString department) {
      this.department = department;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }

  }

}
