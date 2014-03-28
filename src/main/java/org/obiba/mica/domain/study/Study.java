package org.obiba.mica.domain.study;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.obiba.mica.domain.AbstractTimestampedDocument;
import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.Authorization;
import org.obiba.mica.domain.Contact;
import org.obiba.mica.domain.LocalizableString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A Study.
 */
@Document
public class Study extends AbstractTimestampedDocument implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  public enum Access {
    data, bio_samples, other
  }

  @Id
  private String id;

  @Version
  private Long version;

  @NotNull
  private LocalizableString name;

  private LocalizableString acronym;

  private List<Contact> investigators;

  private List<Contact> contacts;

  @NotNull
  private LocalizableString objectives;

  @URL
  private String website;

  private Authorization specificAuthorization;

  private Authorization maelstromAuthorization;

  private StudyMethods methods;

  private NumberOfParticipants numberOfParticipants;

  private Integer startYear;

  private Integer endYear;

  private List<String> access;

  private LocalizableString otherAccess;

  private String markerPaper;

  //TODO add pubmedId validator
  private String pubmedId;

  private List<Attachment> attachments;

  private LocalizableString infos;

  private List<Population> populations;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public LocalizableString getName() {
    return name;
  }

  public void setName(LocalizableString name) {
    this.name = name;
  }

  public LocalizableString getAcronym() {
    return acronym;
  }

  public void setAcronym(LocalizableString acronym) {
    this.acronym = acronym;
  }

  @NotNull
  public List<Contact> getInvestigators() {
    return investigators == null ? (investigators = new ArrayList<>()) : investigators;
  }

  public void setInvestigators(List<Contact> investigators) {
    this.investigators = investigators;
  }

  @NotNull
  public List<Contact> getContacts() {
    return contacts == null ? (contacts = new ArrayList<>()) : contacts;
  }

  public void setContacts(List<Contact> contacts) {
    this.contacts = contacts;
  }

  public LocalizableString getObjectives() {
    return objectives;
  }

  public void setObjectives(LocalizableString objectives) {
    this.objectives = objectives;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public Authorization getSpecificAuthorization() {
    return specificAuthorization;
  }

  public void setSpecificAuthorization(Authorization specificAuthorization) {
    this.specificAuthorization = specificAuthorization;
  }

  public Authorization getMaelstromAuthorization() {
    return maelstromAuthorization;
  }

  public void setMaelstromAuthorization(Authorization maelstromAuthorization) {
    this.maelstromAuthorization = maelstromAuthorization;
  }

  public StudyMethods getMethods() {
    return methods;
  }

  public void setMethods(StudyMethods methods) {
    this.methods = methods;
  }

  public NumberOfParticipants getNumberOfParticipants() {
    return numberOfParticipants;
  }

  public void setNumberOfParticipants(NumberOfParticipants numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  public Integer getStartYear() {
    return startYear;
  }

  public void setStartYear(Integer startYear) {
    this.startYear = startYear;
  }

  public Integer getEndYear() {
    return endYear;
  }

  public void setEndYear(Integer endYear) {
    this.endYear = endYear;
  }

  public List<String> getAccess() {
    return access == null ? (access = new ArrayList<>()) : access;
  }

  public void setAccess(List<String> access) {
    this.access = access;
  }

  public LocalizableString getOtherAccess() {
    return otherAccess;
  }

  public void setOtherAccess(LocalizableString otherAccess) {
    this.otherAccess = otherAccess;
  }

  public String getMarkerPaper() {
    return markerPaper;
  }

  public void setMarkerPaper(String markerPaper) {
    this.markerPaper = markerPaper;
  }

  public String getPubmedId() {
    return pubmedId;
  }

  public void setPubmedId(String pubmedId) {
    this.pubmedId = pubmedId;
  }

  public List<Attachment> getAttachments() {
    return attachments == null ? (attachments = new ArrayList<>()) : attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }

  public LocalizableString getInfos() {
    return infos;
  }

  public void setInfos(LocalizableString infos) {
    this.infos = infos;
  }

  public List<Population> getPopulations() {
    return populations == null ? (populations = new ArrayList<>()) : populations;
  }

  public void setPopulations(List<Population> populations) {
    this.populations = populations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((Study) obj).id);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).add("id", id).add("name", name).toString();
  }

  public static class StudyMethods implements Serializable {

    private static final long serialVersionUID = 5984119393358199672L;

    public enum Design {
      case_control, case_only, clinical_trial, cohort_study, cross_sectional, other
    }

    public enum RecruitmentTarget {
      individuals, families, other
    }

    private List<String> designs;

    private LocalizableString otherDesign;

    private LocalizableString followUpInfos;

    private List<String> recruitments;

    private LocalizableString otherRecruitments;

    private LocalizableString infos;

    public List<String> getDesigns() {
      return designs == null ? (designs = new ArrayList<>()) : designs;
    }

    public void setDesigns(List<String> designs) {
      this.designs = designs;
    }

    public LocalizableString getOtherDesign() {
      return otherDesign;
    }

    public void setOtherDesign(LocalizableString otherDesign) {
      this.otherDesign = otherDesign;
    }

    public LocalizableString getFollowUpInfos() {
      return followUpInfos;
    }

    public void setFollowUpInfos(LocalizableString followUpInfos) {
      this.followUpInfos = followUpInfos;
    }

    public List<String> getRecruitments() {
      return recruitments == null ? (recruitments = new ArrayList<>()) : recruitments;
    }

    public void setRecruitments(List<String> recruitments) {
      this.recruitments = recruitments;
    }

    public LocalizableString getOtherRecruitments() {
      return otherRecruitments;
    }

    public void setOtherRecruitments(LocalizableString otherRecruitments) {
      this.otherRecruitments = otherRecruitments;
    }

    public LocalizableString getInfos() {
      return infos;
    }

    public void setInfos(LocalizableString infos) {
      this.infos = infos;
    }
  }
}
