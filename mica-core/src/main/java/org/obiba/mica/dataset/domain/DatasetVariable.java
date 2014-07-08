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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.magma.Variable;
import org.obiba.mica.domain.Attribute;
import org.obiba.mica.domain.AttributeAware;
import org.obiba.mica.domain.Indexable;
import org.obiba.mica.domain.NoSuchAttributeException;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class DatasetVariable implements Indexable, AttributeAware {

  public enum Type {
    STUDY,
    HARMONIZED,
    DATASCHEMA
  }

  @NotNull
  private String datasetId;

  private Type variableType;

  private final String name;

  private final String entityType;

  private final String mimeType;

  private final String unit;

  private final String valueType;

  private final String referencedEntityType;

  private final boolean repeatable;

  private final String occurrenceGroup;

  private List<DatasetCategory> categories;

  private LinkedListMultimap<String, Attribute> attributes;

  public DatasetVariable(StudyDataset dataset, Variable variable) {
    this(dataset.getId(), Type.STUDY, variable);
  }

  public DatasetVariable(HarmonizedDataset dataset, Variable variable) {
    this(dataset.getId(), Type.DATASCHEMA, variable);
  }

  public DatasetVariable(String datasetId, Type type, Variable variable) {
    this.datasetId = datasetId;
    variableType = type;
    name = variable.getName();
    entityType = variable.getEntityType();
    mimeType = variable.getMimeType();
    unit = variable.getUnit();
    valueType = variable.getValueType().getName();
    referencedEntityType = variable.getReferencedEntityType();
    repeatable = variable.isRepeatable();
    occurrenceGroup = variable.getOccurrenceGroup();
    if(variable.hasCategories()) {
      categories = variable.getCategories().stream().map(DatasetCategory::new).collect(Collectors.toList());
    }
    if(variable.hasAttributes()) {
      for(org.obiba.magma.Attribute attr : variable.getAttributes()) {
        addAttribute(Attribute.Builder.newAttribute(attr).build());
      }
    }
  }

  @Override
  public String getId() {
    return datasetId + "_" + name;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Type getVariableType() {
    return variableType;
  }

  public String getName() {
    return name;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getUnit() {
    return unit;
  }

  public String getValueType() {
    return valueType;
  }

  public String getReferencedEntityType() {
    return referencedEntityType;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  public String getOccurrenceGroup() {
    return occurrenceGroup;
  }

  public List<DatasetCategory> getCategories() {
    return categories;
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
}
