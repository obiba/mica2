package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A Network.
 */
@Document
public class Network extends AbstractTimestampedDocument implements Serializable {

  private static final long serialVersionUID = -4271967393906681773L;

  @Id
  private String id;

  @Version
  private Long version;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private List<Contact> investigators;

  private List<Contact> contacts;

  @NotNull
  private LocalizedString description;

  @URL
  private String website;

  private List<Attachment> attachments;

  private LocalizedString infos;

  private List<StudyRelation> studies;

  private Authorization maelstromAuthorization;

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

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getAcronym() {
    return acronym;
  }

  public void setAcronym(LocalizedString acronym) {
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

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  @NotNull
  public List<Attachment> getAttachments() {
    return attachments == null ? (attachments = new ArrayList<>()) : attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }

  public LocalizedString getInfos() {
    return infos;
  }

  public void setInfos(LocalizedString infos) {
    this.infos = infos;
  }

  @NotNull
  public List<StudyRelation> getStudies() {
    return studies == null ? (studies = new ArrayList<>()) : studies;
  }

  public void setStudies(List<StudyRelation> studies) {
    this.studies = studies;
  }

  public Authorization getMaelstromAuthorization() {
    return maelstromAuthorization;
  }

  public void setMaelstromAuthorization(Authorization maelstromAuthorization) {
    this.maelstromAuthorization = maelstromAuthorization;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((Network) obj).id);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).add("id", id).add("name", name).toString();
  }

  public static class StudyRelation implements Serializable {

    private static final long serialVersionUID = -3544564801184477575L;

    private String studyId;

    private LocalizedString studyName;

    public String getStudyId() {
      return studyId;
    }

    public void setStudyId(String studyId) {
      this.studyId = studyId;
    }

    public LocalizedString getStudyName() {
      return studyName;
    }

    public void setStudyName(LocalizedString studyName) {
      this.studyName = studyName;
    }
  }
}
