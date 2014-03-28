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
import org.obiba.mica.domain.LocalizedString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A Study.
 */
@Document
public class Study extends AbstractTimestampedDocument implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

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

  private List<NetworkRelation> networks;

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
    return access == null ? (access = new ArrayList<>()) : access;
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

  public List<NetworkRelation> getNetworks() {
    return networks;
  }

  public void setNetworks(List<NetworkRelation> networks) {
    this.networks = networks;
  }

  public static class StudyMethods implements Serializable {

    private static final long serialVersionUID = 5984119393358199672L;

    private List<String> designs;

    private LocalizedString otherDesign;

    private LocalizedString followUpInfos;

    private List<String> recruitments;

    private LocalizedString otherRecruitments;

    private LocalizedString infos;

    public List<String> getDesigns() {
      return designs == null ? (designs = new ArrayList<>()) : designs;
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

    public LocalizedString getFollowUpInfos() {
      return followUpInfos;
    }

    public void setFollowUpInfos(LocalizedString followUpInfos) {
      this.followUpInfos = followUpInfos;
    }

    public List<String> getRecruitments() {
      return recruitments == null ? (recruitments = new ArrayList<>()) : recruitments;
    }

    public void setRecruitments(List<String> recruitments) {
      this.recruitments = recruitments;
    }

    public LocalizedString getOtherRecruitments() {
      return otherRecruitments;
    }

    public void setOtherRecruitments(LocalizedString otherRecruitments) {
      this.otherRecruitments = otherRecruitments;
    }

    public LocalizedString getInfos() {
      return infos;
    }

    public void setInfos(LocalizedString infos) {
      this.infos = infos;
    }
  }

  public static class NetworkRelation implements Serializable {

    private static final long serialVersionUID = -3544564801184477575L;

    private String networkId;

    private LocalizedString networkName;

    public String getNetworkId() {
      return networkId;
    }

    public void setNetworkId(String networkId) {
      this.networkId = networkId;
    }

    public LocalizedString getNetworkName() {
      return networkName;
    }

    public void setNetworkName(LocalizedString networkName) {
      this.networkName = networkName;
    }
  }
}
