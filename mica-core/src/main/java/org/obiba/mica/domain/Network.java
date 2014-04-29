package org.obiba.mica.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.domain.Persistable;

/**
 * A Network.
 */
public class Network implements Persistable<String> {

  private static final long serialVersionUID = -4271967393906681773L;

  private String id;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private List<Contact> investigators;

  private List<Contact> contacts;

  private LocalizedString description;

  @URL
  private String website;

  private List<Attachment> attachments;

  private LocalizedString infos;

  private List<String> studyIds;

  private transient List<Study> studies;

  private Authorization maelstromAuthorization;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return id == null;
  }

  public void setId(String id) {
    this.id = id;
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
    return investigators;
  }

  public void addInvestigator(@NotNull Contact investigator) {
    if (investigators == null) investigators = new ArrayList<>();
    investigators.add(investigator);
  }

  public void setInvestigators(List<Contact> investigators) {
    this.investigators = investigators;
  }

  @NotNull
  public List<Contact> getContacts() {
    return contacts;
  }

  public void addContact(@NotNull Contact contact) {
    if (contacts == null) contacts = new ArrayList<>();
    contacts.add(contact);
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

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void addAttachment(@NotNull Attachment attachment) {
    if (attachments == null) attachments = new ArrayList<>();
    attachments.add(attachment);
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

  public List<String> getStudyIds() {
    return studyIds;
  }

  public void addStudyId(@NotNull String studyId) {
    if (studyIds == null) studyIds = new ArrayList<>();
    studyIds.add(studyId);
  }

  public void setStudyIds(List<String> studyIds) {
    this.studyIds = studyIds;
  }

  @NotNull
  public List<Study> getStudies() {
    return studies;
  }

  public void addStudy(@NotNull Study study) {
    if (studies == null) studies = new ArrayList<>();
    studies.add(study);
  }

  public void setStudies(List<Study> studies) {
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

}
