package org.obiba.mica.core.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import com.google.common.base.Strings;

public class Contact implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  private String title;

  private String firstName;

  @NotBlank
  private String lastName;

  private String academicLevel;

  @Email
  private String email;

  private String phone;

  private boolean dataAccessCommitteeMember;

  private Institution institution;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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

  public String getAcademicLevel() {
    return academicLevel;
  }

  public void setAcademicLevel(String academicLevel) {
    this.academicLevel = academicLevel;
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

  public void cleanContact() {
    if (institution != null && institution.getName() != null && !Strings.isNullOrEmpty(lastName)) {
      institution.getName().values().forEach(name -> lastName = lastName.replace("(" + name +")", ""));
      lastName = lastName.trim();
    }
    if (!Strings.isNullOrEmpty(lastName)) {
      if (Strings.isNullOrEmpty(academicLevel) && lastName.contains(",")) {
        int idx = lastName.indexOf(',');
        academicLevel = lastName.substring(idx + 1).trim();
        lastName = lastName.substring(0, idx);
      }
      String[] tokens = lastName.split(" ");
      if (Strings.isNullOrEmpty(title) && tokens.length > 2) {
        title = tokens[0].trim();
      }
      if(Strings.isNullOrEmpty(firstName) && tokens.length > 1) {
        firstName = tokens[tokens.length > 2 ? 1 : 0].trim();
      }
      if (tokens.length > 1) {
        lastName = "";
        int from = tokens.length > 2 ? 2 : 1;
        for (int i = from; i < tokens.length; i++) {
          lastName = lastName + (i == from ? "" : " ") + tokens[i];
        }
      }
    }
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
    private LocalizedString name;

    private LocalizedString department;

    private Address address;

    public LocalizedString getName() {
      return name;
    }

    public void setName(LocalizedString name) {
      this.name = name;
    }

    public LocalizedString getDepartment() {
      return department;
    }

    public void setDepartment(LocalizedString department) {
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
