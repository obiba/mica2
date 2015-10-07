package org.obiba.mica.network.domain;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.URL;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.Authorization;
import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.domain.ContactAware;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.core.mapping.DBRef;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * A Network.
 */
public class Network extends AbstractAuditableDocument implements AttributeAware, ContactAware {

  private static final long serialVersionUID = -4271967393906681773L;

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private boolean published = false;

  @DBRef
  private List<Contact> investigators = Lists.newArrayList();

  @DBRef
  private List<Contact> contacts = Lists.newArrayList();

  private LocalizedString description;

  @URL
  private String website;

  private LocalizedString infos;

  private List<String> studyIds;

  private Authorization maelstromAuthorization;

  private Attachment logo;

  private Attributes attributes;

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

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  @NotNull
  public List<Contact> getInvestigators() {
    return investigators;
  }

  public void addInvestigator(@NotNull Contact investigator) {
    investigators.add(investigator);
    investigator.addNetwork(this);
  }

  public void setInvestigators(List<Contact> investigators) {
    if (investigators == null) investigators = Lists.newArrayList();

    this.investigators = investigators;
    this.investigators.forEach(c -> c.addNetwork(this));
  }

  @NotNull
  public List<Contact> getContacts() {
    return contacts;
  }

  public void addContact(Contact contact) {
    contacts.add(contact);
    contact.addNetwork(this);
  }

  public void addToContact(Contact contact) {
    contact.addNetwork(this);
  }

  public void removeFromContact(Contact contact) {
    contact.removeNetwork(this);
  }

  public void setContacts(List<Contact> contacts) {
    if (investigators == null) investigators = Lists.newArrayList();

    this.contacts = contacts;
    this.contacts.forEach(c -> c.addNetwork(this));
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
    if (!Strings.isNullOrEmpty(website) && !website.startsWith("http")) {
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
    if (!study.isNew()) addStudyId(study.getId());
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
  public Iterable<Contact> getAllContacts() {
    return Iterables.concat(contacts, investigators);
  }
}
