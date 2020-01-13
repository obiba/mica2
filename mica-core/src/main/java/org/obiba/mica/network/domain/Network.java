/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.obiba.mica.core.domain.AbstractModelAware;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.PersonAware;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.domain.Study;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A Network.
 */
public class Network extends AbstractModelAware implements AttributeAware, PersonAware {

  private static final long serialVersionUID = -4271967393906681775L;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private boolean published = false;

  private Map<String, List<String>> membershipSortOrder = new HashMap<>();

  private Map<String, List<Membership>> memberships = new HashMap<String, List<Membership>>() {
    {
      put(Membership.CONTACT, Lists.newArrayList());
      put(Membership.INVESTIGATOR, Lists.newArrayList());
    }
  };

  private LocalizedString description;

  @URL
  private String website;

  private LocalizedString infos;

  private List<String> studyIds;

  private Authorization maelstromAuthorization;

  private Attachment logo;

  private Attributes attributes;

  private List<String> networkIds = Lists.newArrayList();

  private long numberOfStudies;

  @URL
  private String opal;

  //
  // Accessors
  //

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

  public Map<String, List<String>> getMembershipSortOrder() {
    return membershipSortOrder;
  }

  public void setMembershipSortOrder(Map<String, List<String>> membershipSortOrder) {
    this.membershipSortOrder = membershipSortOrder;
  }

  /**
   * @deprecated kept for backward compatibility.
   * @return
     */
  @Deprecated
  @JsonIgnore
  public boolean isPublished() {
    return published;
  }

  /**
   * @deprecated kept for backward compatibility.
   * @param published
     */
  @Deprecated
  @JsonProperty
  public void setPublished(boolean published) {
    this.published = published;
  }

  @Override
  public void addToPerson(Membership membership) {
    membership.getPerson().addNetwork(this, membership.getRole());
  }

  @Override
  public void removeFromPerson(Membership membership) {
    membership.getPerson().removeNetwork(this, membership.getRole());
  }

  @Override
  public void removeFromPerson(Person person) {
    person.removeNetwork(this);
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
    if(!Strings.isNullOrEmpty(website) && !website.startsWith("http")) {
      this.website = "http://" + website;
    }
  }

  public LocalizedString getInfos() {
    return infos;
  }

  public void setInfos(LocalizedString infos) {
    this.infos = infos;
  }

  @NotNull
  public List<String> getStudyIds() {
    if(studyIds == null) studyIds = new ArrayList<>();
    return studyIds;
  }

  public void addStudyId(@NotNull String studyId) {
    getStudyIds().add(studyId);
  }

  public void setStudyIds(List<String> studyIds) {
    this.studyIds = studyIds;
  }

  public void addStudy(@NotNull Study study) {
    if(!study.isNew()) addStudyId(study.getId());
  }

  public Authorization getMaelstromAuthorization() {
    return maelstromAuthorization;
  }

  public void setNumberOfStudies(long numberOfStudies) {
    this.numberOfStudies = numberOfStudies;
  }

  public long getNumberOfStudies() {
    return numberOfStudies;
  }

  public void setMaelstromAuthorization(Authorization maelstromAuthorization) {
    this.maelstromAuthorization = maelstromAuthorization;
  }

  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name);
  }

  public Attachment getLogo() {
    return this.logo;
  }

  public void setLogo(Attachment attachment) {
    this.logo = attachment;
  }

  public List<Person> removeRole(String role) {
    List<Membership> members = this.memberships.getOrDefault(role, Lists.newArrayList());
    this.memberships.remove(role);
    return members.stream().map(m -> {
      m.getPerson().removeNetwork(this, role);
      return m.getPerson();
    }).collect(toList());
  }

  public Set<String> membershipRoles() {
    return this.memberships.keySet();
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void setAttributes(Attributes value) {
    attributes = value;
  }

  @Override
  public Map<String, Object> getModel() {
    if (!this.hasModel()) {
      Map<String, Object> map = Maps.newHashMap();

      if (this.getWebsite() != null) map.put("website", this.getWebsite());
      if (this.getInfos() != null) map.put("infos", this.getInfos());
      if (this.getMaelstromAuthorization() != null) map.put("maelstromAuthorization", this.getMaelstromAuthorization());
      if (getAttributes() != null) getAttributes().forEach(map::put);

      this.setModel(map);
    }

    return super.getModel();
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
  public List<Person> getAllPersons() {
    return getMemberships().values().stream().flatMap(List::stream).map(Membership::getPerson).distinct()
      .collect(toList());
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
    List<Membership> oldMemberships = memberships.get(Membership.INVESTIGATOR);
    Set<Membership> newMemberships = investigators.stream().map(investigator -> new Membership(investigator, Membership.INVESTIGATOR)).collect(toSet());
    newMemberships.addAll(oldMemberships);

    memberships.put(Membership.INVESTIGATOR, new ArrayList<>(newMemberships));
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
    List<Membership> oldMemberships = memberships.get(Membership.CONTACT);
    Set<Membership> newMemberships = contacts.stream().map(contact -> new Membership(contact, Membership.CONTACT)).collect(toSet());
    newMemberships.addAll(oldMemberships);

    memberships.put(Membership.CONTACT, new ArrayList<>(newMemberships));
  }

  @Override
  public List<Membership> getAllMemberships() {
    return getMemberships().values().stream().flatMap(List::stream).collect(toList());
  }

  public Map<String, List<Membership>> getMemberships() {
    return memberships;
  }

  public void setMemberships(Map<String, List<Membership>> memberships) {
    if (memberships == null) {
      this.memberships.clear();
    } else {
      this.memberships = memberships;
    }
  }

  @Override
  public String pathPrefix() {
    return "networks";
  }

  @Override
  public Map<String, Serializable> parts() {
    Network self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  public String getOpal() {
    return opal;
  }

  public void setOpal(String opal) {
    this.opal = opal;
  }

  public List<String> getNetworkIds() {
    return networkIds;
  }

  public void setNetworkIds(@NotNull List<String> networkIds) {
    this.networkIds = networkIds;
  }
}
