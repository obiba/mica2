package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.domain.Persistable;

/**
 * A Study.
 */
public class Study implements Persistable<String> {

  private static final long serialVersionUID = 6559914069652243954L;

  private String id;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private List<Contact> investigators;

  private List<Contact> contacts;

  private LocalizedString objectives;

  @URL
  private String website;

  private Authorization specificAuthorization;

  private Authorization maelstromAuthorization;

  private StudyMethods methods;

  private NumberOfParticipants numberOfParticipants;

  private Integer startYear;

  private Integer endYear;

  private List<String> access;

  private LocalizedString otherAccess;

  private String markerPaper;

  //TODO add pubmedId validator
  private String pubmedId;

  private List<Attachment> attachments;

  private LocalizedString infos;

  private List<Population> populations;

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

  public void addContact(@NotNull Contact aContact) {
    if (contacts == null) contacts = new ArrayList<>();
    contacts.add(aContact);
  }

  public void setContacts(List<Contact> contacts) {
    this.contacts = contacts;
  }

  public LocalizedString getObjectives() {
    return objectives;
  }

  public void setObjectives(LocalizedString objectives) {
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
    return access;
  }

  public void addAccess(@NotNull String anAccess) {
    if (access == null) access = new ArrayList<>();
    access.add(anAccess);
  }

  public void setAccess(List<String> access) {
    this.access = access;
  }

  public LocalizedString getOtherAccess() {
    return otherAccess;
  }

  public void setOtherAccess(LocalizedString otherAccess) {
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
    return attachments;
  }

  public void addAttachment(@NotNull Attachment anAttachment) {
    if (attachments == null) attachments = new ArrayList<>();
    attachments.add(anAttachment);
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

  public List<Population> getPopulations() {
    return populations;
  }

  public void addPopulation(@NotNull Population population) {
    if (populations == null) populations = new ArrayList<>();
    populations.add(population);
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

    private List<String> designs;

    private LocalizedString otherDesign;

    private LocalizedString followUpInfo;

    private List<String> recruitments;

    private LocalizedString otherRecruitment;

    private LocalizedString info;

    public List<String> getDesigns() {
      return designs;
    }

    public void addDesign(@NotNull String design) {
      if (designs == null) designs = new ArrayList<>();
      designs.add(design);
    }

    public void setDesigns(List<String> designs) {
      this.designs = designs;
    }

    public LocalizedString getOtherDesign() {
      return otherDesign;
    }

    public void setOtherDesign(LocalizedString otherDesign) {
      this.otherDesign = otherDesign;
    }

    public LocalizedString getFollowUpInfo() {
      return followUpInfo;
    }

    public void setFollowUpInfo(LocalizedString followUpInfo) {
      this.followUpInfo = followUpInfo;
    }

    public List<String> getRecruitments() {
      return recruitments;
    }

    public void addRecruitment(@NotNull String recruitment) {
      if (recruitments == null) recruitments = new ArrayList<>();
      recruitments.add(recruitment);
    }

    public void setRecruitments(List<String> recruitments) {
      this.recruitments = recruitments;
    }

    public LocalizedString getOtherRecruitment() {
      return otherRecruitment;
    }

    public void setOtherRecruitment(LocalizedString otherRecruitment) {
      this.otherRecruitment = otherRecruitment;
    }

    public LocalizedString getInfo() {
      return info;
    }

    public void setInfo(LocalizedString info) {
      this.info = info;
    }
  }

}
