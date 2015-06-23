package org.obiba.mica.study.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.PersistableWithAttachments;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.date.PersitableYear;
import org.springframework.data.mongodb.core.mapping.DBRef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A Study.
 */
public class Study extends AbstractGitPersistable implements AttributeAware, PersistableWithAttachments {

  private static final long serialVersionUID = 6559914069652243954L;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private Attachment logo;

  private List<Contact> investigators;

  private List<Contact> contacts;

  private LocalizedString objectives;

  @URL
  private String website;

  private Authorization specificAuthorization;

  private Authorization maelstromAuthorization;

  private StudyMethods methods;

  private NumberOfParticipants numberOfParticipants;

  private PersitableYear start;

  private PersitableYear end;

  private List<String> access;

  private LocalizedString otherAccess;

  private String markerPaper;

  //TODO add pubmedId validator
  //@PubmedId
  private String pubmedId;

  @DBRef
  private List<Attachment> attachments = Lists.newArrayList();

  private Iterable<Attachment> removedAttachments = Lists.newArrayList();

  private LocalizedString info;

  private SortedSet<Population> populations = Sets.newTreeSet();

  @URL
  private String opal;

  private Attributes attributes;

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

  public Attachment getLogo() {
    return logo;
  }

  public void setLogo(Attachment logo) {
    this.logo = logo;
  }

  public List<Contact> getInvestigators() {
    return investigators;
  }

  public void addInvestigator(@NotNull Contact investigator) {
    if(investigators == null) investigators = new ArrayList<>();
    investigators.add(investigator);
  }

  public void setInvestigators(List<Contact> investigators) {
    this.investigators = investigators;
  }

  public List<Contact> getContacts() {
    return contacts;
  }

  public void addContact(@NotNull Contact contact) {
    if(contacts == null) contacts = new ArrayList<>();
    contacts.add(contact);
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
    if (!Strings.isNullOrEmpty(website) && !website.startsWith("http")) {
      this.website = "http://" + website;
    }
  }

  public String getOpal() {
    return opal;
  }

  public void setOpal(String opal) {
    this.opal = opal;
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

  public Integer getStart() {
    return start == null ? null : start.getYear();
  }

  public void setStart(Integer value) {
    if (value == null) return;
    if (start == null) start = new PersitableYear();
    start.setYear(value);
  }

  public Integer getEnd() {
    return end == null ? null : end.getYear();
  }

  public void setEnd(Integer value) {
    if (value == null) return;
    if (end == null) end = new PersitableYear();
    end.setYear(value);
  }

  public List<String> getAccess() {
    return access;
  }

  public void addAccess(@NotNull String anAccess) {
    if(access == null) access = new ArrayList<>();
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

  @Override
  public boolean hasAttachments() {
    return attachments != null && !attachments.isEmpty();
  }

  @Override
  public List<Attachment> getAttachments() {
    return attachments;
  }

  @Override
  public void addAttachment(@NotNull Attachment anAttachment) {
    attachments.add(anAttachment);
  }

  @Override
  public void setAttachments(List<Attachment> attachments) {
    removedAttachments = Sets.difference(Sets.newHashSet(this.attachments), Sets.newHashSet(attachments));
    this.attachments = attachments;
  }

  @Override
  public List<Attachment> removedAttachments() {
    return Lists.newArrayList(removedAttachments);
  }

  @NotNull
  @Override
  public Attachment findAttachmentById(String attachmentId) {
    if(getLogo() != null && logo.getId().equals(attachmentId)) return logo;
    if(getAttachments() != null) {
      for(Attachment attachment : getAttachments()) {
        if(attachment.getId().equals(attachmentId)) return attachment;
      }
    }
    if(getPopulations() != null) {
      for(Population population : getPopulations().stream().filter(p -> p.getDataCollectionEvents() != null)
          .collect(Collectors.toList())) {
        for(DataCollectionEvent dce : population.getDataCollectionEvents().stream()
            .filter(d -> d.getAttachments() != null).collect(Collectors.toList())) {
          for(Attachment attachment : dce.getAttachments()) {
            if(attachment.getId().equals(attachmentId)) return attachment;
          }
        }
      }
    }
    throw new NoSuchElementException("Attachment " + attachmentId + " not found for study " + getId());
  }

  @JsonIgnore
  @Override
  public Iterable<Attachment> getAllAttachments() {
    Collection<Attachment> all = new ArrayList<>();
    if(logo != null) {
      all.add(logo);
    }
    if(getAttachments() != null) {
      all.addAll(getAttachments());
    }
    if(getPopulations() != null) {
      getPopulations().stream() //
          .filter(population -> population.getDataCollectionEvents() != null) //
          .forEach(population -> population.getDataCollectionEvents().stream() //
              .filter(dce -> dce.getAttachments() != null) //
              .forEach(dce -> all.addAll(dce.getAttachments())));
    }

    return Sets.newHashSet(all);
  }

  public LocalizedString getInfo() {
    return info;
  }

  public void setInfo(LocalizedString info) {
    this.info = info;
  }

  public SortedSet<Population> getPopulations() {
    return populations;
  }

  public void addPopulation(@NotNull Population population) {
    if(populations == null) populations = new TreeSet<>();
    if(population.isNew()) {
      String newId = population.getName().asAcronym().asString().toLowerCase();
      if(hasPopulation(newId)) {
        for(int i = 1; i < 1000; i++) {
          if(!hasPopulation(newId + "_" + i)) {
            population.setId(newId + "_" + i);
            break;
          }
        }
      } else population.setId(newId);
    }
    populations.add(population);
  }

  public boolean hasPopulation(String populationId) {
    if(populations == null) return false;
    for(Population population : populations) {
      if(population.getId().equals(populationId)) return true;
    }
    return false;
  }

  public boolean hasPopulations() {
    return populations != null && !populations.isEmpty();
  }

  public void setPopulations(SortedSet<Population> newPopulations) {
    if (newPopulations == null) {
      // during serialization input can be null
      populations = newPopulations;
      return;
    }

    // make sure we don't keep old entries
    populations = new TreeSet<>();
    newPopulations.forEach(this::addPopulation);
  }

  /**
   * Make the {@link org.obiba.mica.study.domain.Population} IDs sequential.
   */
  public void rebuildPopulationIds() {
    if (populations == null) return;

    Iterable<Population> populationsOriginal = new TreeSet<>(populations);

    populations.clear();
    int idx = 1;
    for (Population population : populationsOriginal) {
      population.setId(idx + "");
      idx++;
      population.rebuildDataCollectionEventIds();
      populations.add(population);
    }
  }

  /**
   * For each {@link org.obiba.mica.core.domain.Contact} and investigators: trim strings, make sure institution is
   * not repeated in contact name etc.
   */
  public void cleanContacts() {
    cleanContacts(contacts);
    cleanContacts(investigators);
  }

  private void cleanContacts(List<Contact> contactList) {
    if (contactList == null) return;
    contactList.forEach(Contact::cleanContact);
  }

  @Override
  protected Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name);
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attributes == null) attributes = new Attributes();
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    if(attributes != null) {
      attributes.removeAttribute(attribute);
    }
  }

  @Override
  public void removeAllAttributes() {
    if(attributes != null) attributes.removeAllAttributes();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes != null && attributes.hasAttribute(attName, namespace);
  }

  @Override
  public String pathPrefix() {
    return "studies";
  }

  @Override
  public Map<String, Serializable> parts() {
    Study self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
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
      if(designs == null) designs = new ArrayList<>();
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
      if(recruitments == null) recruitments = new ArrayList<>();
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
