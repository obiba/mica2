/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import com.google.common.collect.Maps;
import org.obiba.mica.core.domain.AbstractModelAware;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.core.domain.LocalizedString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.obiba.mica.spi.tables.IDataset;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Proxy to value tables.
 */
public abstract class Dataset extends AbstractModelAware implements AttributeAware, Indexable, IDataset {

  private static final long serialVersionUID = -3328963766855899217L;

  public static final String DEFAULT_ENTITY_TYPE = "Participant";

  public static final String MAPPING_NAME = "Dataset";

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private LocalizedString description;

  private String entityType = DEFAULT_ENTITY_TYPE;

  private boolean published = false;

  private Attributes attributes;

  private Set<Attribute> inferredAttributes = new HashSet<>();

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

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType == null ? DEFAULT_ENTITY_TYPE : entityType;
  }

  /**
   * @deprecated kept for backward compatibility.
   * @return
     */
  @JsonIgnore
  @Deprecated
  public boolean isPublished() {
    return published;
  }

  @JsonProperty
  public void setPublished(boolean published) {
    this.published = published;
  }

  public void setAttributes(Attributes attributes) {
    this.attributes = attributes;
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
  public String getClassName() {
    return getClass().getSimpleName();
  }

  // for JSON deserial
  public void setClassName(String className) {}

  @Override
  @JsonIgnore
  public String getMappingName() {
    return MAPPING_NAME;
  }

  @Override
  @JsonIgnore
  public String getParentId() {
    return null;
  }

  @Override
  public Map<String, Object> getModel() {
    if (!this.hasModel()) {
      Map<String, Object> map = Maps.newHashMap();
      if (getAttributes() != null) getAttributes().forEach(map::put);
      setModel(map);
    }
    return super.getModel();
  }

  @Override
  protected MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("name", name);
  }

  public Set<Attribute> getInferredAttributes() {
    return inferredAttributes;
  }

  public void setInferredAttributes(Set<Attribute> inferredAttributes) {
    this.inferredAttributes = inferredAttributes == null ? new HashSet<>() : inferredAttributes;
  }
}
