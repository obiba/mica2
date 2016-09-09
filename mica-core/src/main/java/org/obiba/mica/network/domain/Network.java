/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.validator.constraints.URL;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.AbstractModelAware;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.domain.PersonAware;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.domain.Study;

import static java.util.stream.Collectors.toList;

/**
 * A Network.
 */
public class Network extends AbstractModelAware implements AttributeAware, PersonAware {

  private static final long serialVersionUID = -4271967393906681773L;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private boolean published = false;

  private List<Person> investigators = Lists.newArrayList();

  private List<Person> contacts = Lists.newArrayList();

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

  @NotNull
  @JsonIgnore
  public List<Person> getInvestigators() {
    if(!investigators.isEmpty()) {
      setInvestigators(investigators);
      investigators.clear();
    }

    return memberships.getOrDefault(Membership.INVESTIGATOR, Lists.newArrayList()).stream().map(m -> m.getPerson())
      .collect(toList());
  }

  @JsonProperty
  public void setInvestigators(List<Person> investigators) {
    if(investigators == null) investigators = Lists.newArrayList();

    replaceExisting(investigators);

    memberships.put(Membership.INVESTIGATOR,
      investigators.stream().map(p -> new Membership(p, Membership.INVESTIGATOR)).collect(toList()));
  }

  @NotNull
  @JsonIgnore
  public List<Person> getContacts() {
    if(!contacts.isEmpty()) {
      setContacts(contacts);
      contacts.clear();
    }

    return memberships.getOrDefault(Membership.CONTACT, Lists.newArrayList()).stream().map(m -> m.getPerson())
      .collect(toList());
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

  @JsonProperty
  public void setContacts(List<Person> contacts) {
    if(contacts == null) contacts = Lists.newArrayList();

    replaceExisting(contacts);

    memberships.put(Membership.CONTACT,
      contacts.stream().map(p -> new Membership(p, Membership.CONTACT)).collect(toList()));
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

  /**
   * For each {@link org.obiba.mica.core.domain.Person} and investigators: trim strings, make sure institution is
   * not repeated in contact name etc.
   */
  public void cleanContacts() {
    cleanContacts(contacts);
    cleanContacts(investigators);
  }

  private void cleanContacts(List<Person> contactList) {
    if(contactList == null) return;
    contactList.forEach(Person::cleanPerson);
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void setAttributes(Attributes value) {
    attributes = value;
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

  @Override
  public List<Membership> getAllMemberships() {
    return getMemberships().values().stream().flatMap(List::stream).collect(toList());
  }

  public Map<String, List<Membership>> getMemberships() {
    if(!contacts.isEmpty()){
      setContacts(contacts);
      contacts.clear();
    }

    if(!investigators.isEmpty()) {
      setInvestigators(investigators);
      investigators.clear();
    }

    return memberships;
  }

  public void setMemberships(Map<String, List<Membership>> memberships) {
    this.memberships = memberships;
    Map<String, Person> seen = Maps.newHashMap();

    this.memberships.entrySet().forEach(e -> e.getValue().forEach(m -> {
      if(seen.containsKey(m.getPerson().getId())) {
        m.setPerson(seen.get(m.getPerson().getId()));
      } else if(!m.getPerson().isNew()) {
        seen.put(m.getPerson().getId(), m.getPerson());
      }
    }));
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

  private void replaceExisting(List<Person> persons) {
    List<Person> existing = this.memberships.values().stream().flatMap(List::stream).map(Membership::getPerson)
      .distinct().collect(toList());

    ImmutableList.copyOf(persons).forEach(p -> {
      if(existing.contains(p)) {
        int idx = persons.indexOf(p);
        persons.remove(p);
        persons.add(idx, existing.get(existing.indexOf(p)));
      }
    });
  }

  public List<String> getNetworkIds() {
    return networkIds;
  }

  public void setNetworkIds(@NotNull List<String> networkIds) {
    this.networkIds = networkIds;
  }
}
