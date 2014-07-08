/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import java.util.Locale;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.AbstractAuditableDocument;
import org.obiba.mica.domain.Attribute;
import org.obiba.mica.domain.AttributeAware;
import org.obiba.mica.domain.Indexable;
import org.obiba.mica.domain.LocalizedString;
import org.obiba.mica.domain.NoSuchAttributeException;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * Proxy to Opal tables.
 */
public abstract class Dataset extends AbstractAuditableDocument implements AttributeAware, Indexable {

  private static final long serialVersionUID = -3328963766855899217L;

  public static final String MAPPING_NAME = "Dataset";

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  private boolean published = false;

  private LinkedListMultimap<String, Attribute> attributes;

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  @Override
  public Multimap<String, Attribute> getAttributes() {
    return attributes;
  }

  @Override
  public void setAttributes(LinkedListMultimap<String, Attribute> attributes) {
    this.attributes = attributes;
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attributes == null) attributes = LinkedListMultimap.create();
    attributes.put(attribute.getMapKey(), attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    if(attributes != null) {
      attributes.remove(attribute.getMapKey(), attribute);
    }
  }

  @Override
  public void removeAllAttributes() {
    if(attributes != null) attributes.clear();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes != null && attributes.containsKey(Attribute.getMapKey(attName, namespace));
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace, @Nullable Locale locale) {
    try {
      getAttribute(attName, namespace, locale);
      return true;
    } catch(NoSuchAttributeException e) {
      return false;
    }
  }

  @Override
  public Attribute getAttribute(String attName, @Nullable String namespace, @Nullable Locale locale)
      throws NoSuchAttributeException {
    if(hasAttribute(attName, namespace)) {
      for(Attribute attribute : attributes.get(Attribute.getMapKey(attName, namespace))) {
        if(attribute.isLocalisedWith(locale)) {
          return attribute;
        }
      }
    }
    throw new NoSuchAttributeException(attName, getClass().getName());
  }

  @Override
  public String getClassName() {
    return getClass().getSimpleName();
  }

  @Override
  public String getMappingName() {
    return MAPPING_NAME;
  }

  @Override
  protected com.google.common.base.Objects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name);
  }
}