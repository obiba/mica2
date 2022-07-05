/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.BaseStudy;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static java.util.stream.Collectors.toList;

@Document
public class Person extends AbstractGitPersistable {

  private static final long serialVersionUID = -3098622168836970902L;

  private String title;

  private String firstName;

  @NotBlank
  private String lastName;

  private String academicLevel;

  private String email;

  private String phone;

  private boolean dataAccessCommitteeMember;

  private Institution institution;

  private List<Membership> studyMemberships = Lists.newArrayList();

  private List<Membership> networkMemberships = Lists.newArrayList();

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

  public String getFullName() {
    if (Strings.isNullOrEmpty(firstName)) return lastName;
    if (Strings.isNullOrEmpty(lastName)) return "";
    return firstName + " " + lastName;
  }

  /**
   * For JSON serialization only.
   * @param fullName
   */
  public void setFullName(String fullName) {
    // ignore
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

  public void cleanPerson() {
    if(institution != null && institution.getName() != null && !Strings.isNullOrEmpty(lastName)) {
      institution.getName().values().forEach(name -> lastName = lastName.replace("(" + name + ")", ""));
      lastName = lastName.trim();
    }
    if(!Strings.isNullOrEmpty(lastName)) {
      if(Strings.isNullOrEmpty(academicLevel) && lastName.contains(",")) {
        int idx = lastName.indexOf(',');
        academicLevel = lastName.substring(idx + 1).trim();
        lastName = lastName.substring(0, idx);
      }
      String[] tokens = lastName.split(" ");
      if(Strings.isNullOrEmpty(title) && tokens.length > 2) {
        title = tokens[0].trim();
      }
      if(Strings.isNullOrEmpty(firstName) && tokens.length > 1) {
        firstName = tokens[tokens.length > 2 ? 1 : 0].trim();
      }
      if(tokens.length > 1) {
        lastName = "";
        int from = tokens.length > 2 ? 2 : 1;
        for(int i = from; i < tokens.length; i++) {
          lastName = lastName + (i == from ? "" : " ") + tokens[i];
        }
      }
    }
  }

  public void removeAllMemberships(String role) {
    networkMemberships = networkMemberships.stream().filter(m -> !role.equals(m.getRole())).collect(toList());
    studyMemberships = studyMemberships.stream().filter(m -> !role.equals(m.getRole())).collect(toList());
  }

  public Institution getInstitution() {
    return institution;
  }

  public void setInstitution(Institution institution) {
    this.institution = institution;
  }

  @JsonIgnore
  @Override
  public boolean isNew() {
    return this.getId() == null;
  }

  public List<Membership> getStudyMemberships() {
    return studyMemberships;
  }

  public void setStudyMemberships(List<Membership> studyMemberships) {
    this.studyMemberships = studyMemberships;
  }

  public void addStudy(@NotNull BaseStudy study, @NotNull String role) {
    Membership membership = Membership.withIdAndRole(study.getId(), role);

    if(!studyMemberships.contains(membership)) this.studyMemberships.add(membership);
  }

  public void removeStudy(@NotNull BaseStudy study) {
    studyMemberships = studyMemberships.stream().filter(m -> !m.getParentId().equals(study.getId())).collect(toList());
  }

  public void removeStudy(@NotNull BaseStudy study, @NotNull String role) {
    Membership membership = Membership.withIdAndRole(study.getId(), role);

    if(studyMemberships.contains(membership)) this.studyMemberships.remove(membership);
  }

  public List<Membership> getNetworkMemberships() {
    return networkMemberships;
  }

  public void setNetworkMemberships(List<Membership> networkMemberships) {
    this.networkMemberships = networkMemberships;
  }

  public void addNetwork(@NotNull Network network, @NotNull String role) {
    Membership membership = Membership.withIdAndRole(network.getId(), role);

    if(!networkMemberships.contains(membership)) this.networkMemberships.add(membership);
  }

  public void removeNetwork(@NotNull Network network) {
    networkMemberships = networkMemberships.stream().filter(m -> !m.getParentId().equals(network.getId())).collect(toList());
  }

  public void removeNetwork(@NotNull Network network, @NotNull String role) {
    Membership membership = Membership.withIdAndRole(network.getId(), role);

    if(networkMemberships.contains(membership)) this.networkMemberships.remove(membership);
  }

  @Override
  public boolean equals(Object object) {
    return this == object || (object != null && this.getClass().equals(object.getClass()) &&
      (getId() != null
        ? Objects.equals(getId(), ((Person) object).getId())
        : !Strings.isNullOrEmpty(getEmail()) ? Objects.equals(getEmail(), ((Person) object).getEmail()) : false));
  }

  @Override
  public int hashCode() {
    if(getId() != null) return Objects.hashCode(getId());

    if(!Strings.isNullOrEmpty(getEmail())) return Objects.hashCode(getEmail());

    return super.hashCode();
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

  public static class Membership implements Serializable {
    private static final long serialVersionUID = -1L;

    private String parentId;

    private String role;

    public Membership() {}

    public Membership(String parentId, String role) {
      this.parentId = parentId;
      this.role = role;
    }

    public static Membership withIdAndRole(String parentId, String role) {
      return new Membership(parentId, role);
    }

    public String getParentId() {
      return parentId;
    }

    public void setParentId(String parentId) {
      this.parentId = parentId;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    @Override
    public boolean equals(Object o) {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      Membership that = (Membership) o;

      return Objects.equals(parentId, that.getParentId()) && Objects.equals(role, that.getRole());
    }

    @Override
    public int hashCode() {
      return Objects.hash(parentId, role);
    }
  }

  @Override
  public Map<String, Serializable> parts() {
    Person self = this;
    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  @Override
  public String pathPrefix() {
    return "people";
  }
}
