/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.hibernate.validator.constraints.URL;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.date.PersitableYear;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A collection Study: a study that describes the context (research objectives, populations and more) of collected data.
 */
@Document
public class Study extends BaseStudy implements AttributeAware {

  private static final long serialVersionUID = 6559914069652243954L;

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

  @Transient
  private List<Attachment> attachments = Lists.newArrayList();

  private LocalizedString info;

  private SortedSet<Population> populations = Sets.newTreeSet();

  private Attributes attributes;


  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
    if (!Strings.isNullOrEmpty(website) && !website.startsWith("http")) {
      this.website = "http://" + website;
    }
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

  /**
   * @return
   * @deprecated kept for backward compatibility.
   */
  @Deprecated
  @JsonIgnore
  public List<Attachment> getAttachments() {
    return attachments;
  }

  /**
   * @param attachments
   * @deprecated kept for backward compatibility.
   */
  @Deprecated
  @JsonProperty
  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments == null ? Lists.newArrayList() : attachments;
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
    if (populations == null) populations = new TreeSet<>();
    if (population.isNew()) {
      String newId = population.getName().asAcronym().asUrlSafeString().toLowerCase();
      if (hasPopulation(newId)) {
        for (int i = 1; i < 1000; i++) {
          if (!hasPopulation(newId + "_" + i)) {
            population.setId(newId + "_" + i);
            break;
          }
        }
      } else population.setId(newId);
    }
    populations.add(population);
  }

  public boolean hasPopulation(String populationId) {
    if (populations == null) return false;
    for (Population population : populations) {
      if (population.getId().equals(populationId)) return true;
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


  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", getName());
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if (attributes == null) attributes = new Attributes();
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    if (attributes != null) {
      attributes.removeAttribute(attribute);
    }
  }

  @Override
  public void removeAllAttributes() {
    if (attributes != null) attributes.removeAllAttributes();
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

  @JsonIgnore
  @Deprecated
  public List<Person> getInvestigators() {
    return getMemberships().get(Membership.INVESTIGATOR).stream().map(Membership::getPerson).collect(toList());
  }

  @JsonProperty
  @Deprecated
  public void setInvestigators(List<Person> investigators) {
    if (investigators == null) return;
    List<Membership> oldMemberships = getMemberships().get(Membership.INVESTIGATOR);
    Set<Membership> newMemberships = investigators.stream().map(investigator -> new Membership(investigator, Membership.INVESTIGATOR)).collect(toSet());
    newMemberships.addAll(oldMemberships);

    getMemberships().put(Membership.INVESTIGATOR, new ArrayList<>(newMemberships));
  }

  @JsonIgnore
  @Deprecated
  public List<Person> getContacts() {
    return getMemberships().get(Membership.CONTACT).stream().map(Membership::getPerson).collect(toList());
  }

  @JsonProperty
  @Deprecated
  public void setContacts(List<Person> contacts) {
    if (contacts == null) return;
    List<Membership> oldMemberships = getMemberships().get(Membership.CONTACT);
    Set<Membership> newMemberships = contacts.stream().map(contact -> new Membership(contact, Membership.CONTACT)).collect(toSet());
    newMemberships.addAll(oldMemberships);

    getMemberships().put(Membership.CONTACT, new ArrayList<>(newMemberships));
  }

  @Override
  public Map<String, Object> getModel() {
    if (!hasModel()) {
      Map<String, Object> map = Maps.newHashMap();

      if (getStart() != null) map.put("startYear", getStart());
      if (getEnd() != null) map.put("endYear", getEnd());
      if (getWebsite() != null) map.put("website", getWebsite());
      if (getMethods() != null) map.put("methods", getMethods());
      if (getNumberOfParticipants() != null) map.put("numberOfParticipants", getNumberOfParticipants());
      if (getAccess() != null) map.put("access", getAccess());
      if (getOtherAccess() != null) map.put("otherAccess", getOtherAccess());
      if (getMarkerPaper() != null) map.put("markerPaper", getMarkerPaper());
      if (getPubmedId() != null) map.put("pubmedId", getPubmedId());
      if (getInfo() != null) map.put("info", getInfo());
      if (getOpal() != null) map.put("opal", getOpal());
      if (getMemberships() != null) map.put("memberships", getMemberships());
      if (getSpecificAuthorization() != null) map.put("specificAuthorization", new AuthorizationModel(getSpecificAuthorization()));
      if (getMaelstromAuthorization() != null) map.put("maelstromAuthorization", new AuthorizationModel(getMaelstromAuthorization()));
      if (getMethods() != null && getMethods().getDesigns() != null && getMethods().getDesigns().size() == 1) {
        getMethods().setDesign(getMethods().getDesigns().get(0));
      }
      getPopulations().forEach(Population::getModel);
      getPopulations().forEach(p -> p.getDataCollectionEvents().forEach(DataCollectionEvent::getModel));
      if (getAttributes() != null) getAttributes().forEach(map::put);

      setModel(map);
    }

    return super.getModel();
  }

  public Population findPopulation(String id) {
    return populations.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
  }

  public static class StudyMethods implements Serializable {

    private static final long serialVersionUID = 5984119393358199672L;

    private List<String> designs;

    private String design;

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

    public void setDesign(String design) {
      this.design = design;
    }

    public void setOtherDesign(LocalizedString otherDesign) {
      this.otherDesign = otherDesign;
    }

    public void setFollowUpInfo(LocalizedString followUpInfo) {
      this.followUpInfo = followUpInfo;
    }

    public void addRecruitment(@NotNull String recruitment) {
      if (recruitments == null) recruitments = new ArrayList<>();
      recruitments.add(recruitment);
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
