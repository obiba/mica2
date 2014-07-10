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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class DatasetVariable implements Indexable, AttributeAware {

  private static final long serialVersionUID = -141834508275072637L;

  public static final String MAPPING_NAME = "Variable";

  public enum Type {
    Study,       // variable extracted from a study dataset
    Dataschema,  // variable extracted from a harmonized dataset
    Harmonized   // variable that implements a Datashema variable
  }

  @NotNull
  private String datasetId;

  private List<String> studyIds;

  private Type variableType;

  private String name;

  private String entityType;

  private String mimeType;

  private String unit;

  private String valueType;

  private String referencedEntityType;

  private boolean repeatable;

  private String occurrenceGroup;

  private List<DatasetCategory> categories;

  private LinkedListMultimap<String, Attribute> attributes;

  public DatasetVariable() {}

  public DatasetVariable(StudyDataset dataset, Variable variable) {
    this(dataset.getId(), Type.Study, variable);
    studyIds = Lists.newArrayList(dataset.getStudyTable().getStudyId());
  }

  public DatasetVariable(HarmonizedDataset dataset, Variable variable) {
    this(dataset.getId(), Type.Dataschema, variable);
    studyIds = Lists.newArrayList();
    dataset.getStudyTables().forEach(table -> studyIds.add(table.getStudyId()));
  }

  private DatasetVariable(String datasetId, Type type, Variable variable) {
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
    return datasetId + ":" + name + ":" + variableType;
  }

  public void setId(String id) {
    IdResolver resolver = IdResolver.from(id);
    variableType = resolver.getType();
    datasetId = resolver.getDatasetId();
    name = resolver.getName();
  }

  public String getDatasetId() {
    return datasetId;
  }

  public List<String> getStudyIds() {
    return studyIds;
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
  public List<Attribute> getAttributes(String attName, @Nullable String namespace) {
    if(!hasAttribute(attName, namespace)) throw new NoSuchAttributeException(attName, getClass().getName());
    return attributes.get(Attribute.getMapKey(attName, namespace));
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

  /**
   * For Json deserialization.
   *
   * @param className
   */
  public void setClassName(String className) {}

  protected com.google.common.base.Objects.ToStringHelper toStringHelper() {
    return com.google.common.base.Objects.toStringHelper(this).omitNullValues().add("id", getId());
  }

  @Override
  public final String toString() {
    return toStringHelper().toString();
  }

  @Override
  public String getMappingName() {
    return MAPPING_NAME;
  }

  public static class IdResolver {

    private final String id;

    private final Type type;

    private final String datasetId;

    private final String name;

    public static IdResolver from(String id) {
      return new IdResolver(id);
    }

    private IdResolver(String id) {
      this.id = id;
      datasetId = id.substring(0, id.indexOf(':'));
      String tail = id.substring(id.indexOf(':') + 1);
      name = tail.substring(0, tail.indexOf(':'));
      type = Type.valueOf(tail.substring(tail.indexOf(':') + 1));
    }

    public Type getType() {
      return type;
    }

    public String getDatasetId() {
      return datasetId;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "[" + datasetId + "," + name + "," + type + "]";
    }
  }
}
