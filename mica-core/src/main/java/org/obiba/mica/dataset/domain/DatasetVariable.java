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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.compress.utils.Lists;
import org.obiba.magma.Variable;
import org.obiba.magma.support.VariableNature;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.HarmonizationStudyTable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.core.support.SpecialCharCodec;
import org.obiba.mica.core.support.SpecialCharCodecFactory;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.spi.tables.IVariable;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class DatasetVariable implements Indexable, AttributeAware, IVariable {

  private static final long serialVersionUID = -141834508275072637L;

  public static final String MAPPING_NAME = "Variable";

  public static final String HMAPPING_NAME = "H" + MAPPING_NAME;

  private static final Pattern INVALID_ATTRIBUTE_NAME_CHARS = Pattern.compile("[.]");

  private static final String ID_SEPARATOR = ":";
  public static final String OPAL_STUDY_TABLE_PREFIX = "Study";
  public static final String OPAL_HARMONIZATION_TABLE_PREFIX = "Harmonization";

  public enum Type {
    Collected,  // variable extracted from a collection dataset
    Dataschema,  // variable extracted from a harmonization dataset
    Harmonized   // variable that implements a Datashema variable
  }

  public enum OpalTableType {
    Study,
    Harmonization
  }

  @NotNull
  private String datasetId;

  private String studyId;

  private String populationId;

  private String dceId;

  private Type variableType;

  private String name;

  private String entityType;

  private String mimeType;

  private String unit;

  private String valueType;

  private String referencedEntityType;

  private Boolean repeatable;

  private String occurrenceGroup;

  private List<DatasetCategory> categories;

  private Attributes attributes;

  private String nature;

  private int index;

  private String source;

  private OpalTableType opalTableType;

  @NotNull
  private LocalizedString datasetAcronym;

  @NotNull
  private LocalizedString datasetName;

  private String containerId;

  private int populationWeight;

  private int dataCollectionEventWeight;

  private Set<String> sets;

  private String tableUid;

  public DatasetVariable() {
  }

  public DatasetVariable(StudyDataset dataset, Variable variable) {
    this(dataset, Type.Collected, variable);
    boolean hasStudyTable = dataset.hasStudyTable();
    studyId = hasStudyTable ? dataset.getStudyTable().getStudyId() : null;
    populationId = hasStudyTable ? dataset.getStudyTable().getPopulationUId() : null;
    dceId = hasStudyTable ? dataset.getStudyTable().getDataCollectionEventUId() : null;
    setContainerId(studyId);
  }

  public DatasetVariable(HarmonizationDataset dataset, Variable variable) {
    this(dataset, Type.Dataschema, variable);

    boolean hasStudyTable = dataset.hasHarmonizationTable();
    HarmonizationStudyTable table = dataset.getHarmonizationTable();
    studyId = hasStudyTable ? table.getStudyId() : null;
    populationId = hasStudyTable ? table.getPopulationUId() : null;
    dceId = hasStudyTable ? table.getDataCollectionEventUId() : null;
    setContainerId(table.getStudyId());
  }

  public DatasetVariable(HarmonizationDataset dataset, Variable variable, BaseStudyTable studyTable) {
    this(dataset, Type.Harmonized, variable);

    studyId = studyTable.getStudyId();
    setContainerId(studyId);
    opalTableType = studyTable instanceof StudyTable ? OpalTableType.Study : OpalTableType.Harmonization;

    populationId = studyTable.getPopulationUId();
    dceId = studyTable.getDataCollectionEventUId();

    source = studyTable.getSource();
    setTableUid(studyTable.getTableUniqueId());
  }

  private DatasetVariable(Dataset dataset, Type type, Variable variable) {
    datasetId = dataset.getId();
    datasetAcronym = dataset.getAcronym();
    datasetName = dataset.getName();
    variableType = type;
    name = variable.getName();
    entityType = variable.getEntityType();
    mimeType = variable.getMimeType();
    unit = variable.getUnit();
    valueType = variable.getValueType().getName();
    referencedEntityType = variable.getReferencedEntityType();
    repeatable = variable.isRepeatable();
    occurrenceGroup = variable.getOccurrenceGroup();
    nature = VariableNature.getNature(variable).name();
    index = variable.getIndex();

    if (variable.hasCategories()) {
      categories = variable.getCategories().stream().map(DatasetCategory::new).collect(Collectors.toList());
    }

    if (variable.hasAttributes()) {
      variable.getAttributes().stream()
          .filter(a -> !INVALID_ATTRIBUTE_NAME_CHARS.matcher(a.getName()).find())
          .forEach(a -> addAttribute(Attribute.Builder.newAttribute(a).build()));
    }
  }

  @Override
  public String getId() {
    String id = datasetId + ID_SEPARATOR + SpecialCharCodecFactory.get().encode(name) + ID_SEPARATOR + variableType;

    if (Type.Harmonized == variableType) {
      String entityId = studyId;
      String tableType = opalTableType == OpalTableType.Study ? OPAL_STUDY_TABLE_PREFIX : OPAL_HARMONIZATION_TABLE_PREFIX;
      id = id + ID_SEPARATOR + tableType + ID_SEPARATOR + entityId + ID_SEPARATOR + source;
    }

    return id;
  }

  public void setId(String id) {
    IdResolver resolver = IdResolver.from(id);
    String tableType = resolver.getTableType();
    variableType = resolver.getType();
    datasetId = resolver.getDatasetId();
    name = resolver.getName();
    opalTableType = Strings.isNullOrEmpty(tableType) ? null : OpalTableType.valueOf(tableType);

    if (resolver.hasStudyId()) studyId = resolver.getStudyId();
    if (resolver.hasSource()) source = resolver.getSource();
  }

  public String getDatasetId() {
    return datasetId;
  }

  public LocalizedString getDatasetAcronym() {
    return datasetAcronym;
  }

  public LocalizedString getDatasetName() {
    return datasetName;
  }

  public String getStudyId() {
    return studyId;
  }

  public String getPopulationId() {
    return populationId;
  }

  public String getDceId() {
    return dceId;
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

  @Override
  public String getValueType() {
    return valueType;
  }

  public String getReferencedEntityType() {
    return referencedEntityType;
  }

  public boolean hasRepeatable() {
    return repeatable != null;
  }

  public boolean isRepeatable() {
    return hasRepeatable() ? repeatable : false;
  }

  public String getOccurrenceGroup() {
    return occurrenceGroup;
  }

  @Override
  public boolean hasCategories() {
    return categories != null && !categories.isEmpty();
  }

  public List<DatasetCategory> getCategories() {
    return categories;
  }

  @Override
  public List<String> getCategoryNames() {
    if (!hasCategories()) return Lists.newArrayList();
    return categories.stream().map(DatasetCategory::getName).collect(Collectors.toList());
  }

  @Override
  public DatasetCategory getCategory(String name) {
    if (!hasCategories()) return null;

    for (DatasetCategory category : categories) {
      if (category.getName().equals(name)) return category;
    }

    return null;
  }

  public boolean hasAttributes() {
    return attributes != null && !attributes.isEmpty();
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

  public String getNature() {
    return nature;
  }

  public int getIndex() {
    return index;
  }

  public String getSource() {
    return source;
  }

  public OpalTableType getOpalTableType() {
    return opalTableType;
  }

  @Override
  public String getClassName() {
    return getClass().getSimpleName();
  }

  public Set<String> getSets() {
    return sets;
  }

  public void setSets(Set<String> sets) {
    this.sets = sets;
  }

  public void addSet(String id) {
    if (sets == null) {
      sets = Sets.newLinkedHashSet();
    }
    sets.add(id);
  }

  public void removeSet(String id) {
    if (sets == null) return;
    sets.remove(id);
  }

  public boolean containsSet(String id) {
    if (sets == null) return false;
    return sets.contains(id);
  }

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = cleanStringForSearch(containerId);
  }

  /**
   * For Json deserialization.
   *
   * @param className
   */
  public void setClassName(String className) {
  }

  @Override
  public final String toString() {
    return MoreObjects.toStringHelper(this).omitNullValues().add("id", getId()).toString();
  }

  @Override
  @JsonIgnore
  public String getMappingName() {
    return variableType.equals(Type.Harmonized) ? "H" + MAPPING_NAME : MAPPING_NAME;
  }

  @Override
  @JsonIgnore
  public String getParentId() {
    return variableType.equals(Type.Harmonized)
        ? datasetId + ID_SEPARATOR + name + ID_SEPARATOR + Type.Dataschema
        : null;
  }

  public int getPopulationWeight() {
    return populationWeight;
  }

  public void setPopulationWeight(int populationWeight) {
    this.populationWeight = populationWeight;
  }

  public int getDataCollectionEventWeight() {
    return dataCollectionEventWeight;
  }

  public void setDataCollectionEventWeight(int dataCollectionEventWeight) {
    this.dataCollectionEventWeight = dataCollectionEventWeight;
  }

  private String cleanStringForSearch(String string) {
    return string != null ? string.replace("-", "") : null;
  }

  public String getTableUid() {
    return tableUid;
  }

  public void setTableUid(String tableUid) {
    this.tableUid = tableUid;
  }

  public static class IdEncoderDecoder {


  }

  public static class IdResolver {

    private String id;

    private final Type type;

    private final String datasetId;

    private final String name;

    private final String studyId;

    private final String tableType;

    private final String source;

    public static IdResolver from(String id) {
      return new IdResolver(id);
    }

    public static IdResolver from(String datasetId, String variableName, Type variableType) {
      return from(encode(datasetId, variableName, variableType, null));
    }

    public static String encode(String datasetId, String variableName, Type variableType) {
      return encode(datasetId, variableName, variableType, null, null, null);
    }

    public static String encode(String datasetId, String variableName, Type variableType, String studyId,
                                String source, String tableType) {
      String id = datasetId + ID_SEPARATOR + SpecialCharCodecFactory.get().encode(variableName) + ID_SEPARATOR + variableType;

      String entityId;

      if (Type.Harmonized == variableType) {
        entityId = studyId;
        id = id + ID_SEPARATOR + tableType + ID_SEPARATOR + entityId + ID_SEPARATOR + source;
      }

      return id;
    }

    public static String encode(String datasetId, String variableName, Type variableType, BaseStudyTable studyTable) {
      String tableType = studyTable instanceof StudyTable ? OPAL_STUDY_TABLE_PREFIX : OPAL_HARMONIZATION_TABLE_PREFIX;
      return studyTable == null
          ? encode(datasetId, variableName, variableType)
          : encode(datasetId,
            variableName,
            variableType,
            studyTable.getStudyId(),
            studyTable.getSource(),
            tableType);
    }

    private IdResolver(String id) {
      if (Strings.isNullOrEmpty(id)) throw new IllegalArgumentException("Dataset variable cannot be null or empty");
      SpecialCharCodec specialCharCodec = SpecialCharCodecFactory.get();

      this.id = specialCharCodec.encode(id);
      boolean encoded = !this.id.equals(id);

      String[] parts = id.split(":urn:");

      String[] tokens = parts[0].split(ID_SEPARATOR);
      if (tokens.length < 3) throw new IllegalArgumentException("Not a valid dataset variable ID: " + id);

      datasetId = tokens[0];
      name = encoded ? specialCharCodec.decode(tokens[1]) : tokens[1];
      type = Type.valueOf(tokens[2]);

      tableType = tokens.length > 3 ? tokens[3] : null;
      studyId = tokens.length > 4 ? tokens[4] : null;

      if (parts.length>1) {
        source = "urn:" + parts[1];
      } else if (tokens.length > 6) {
        // legacy
        source = String.format("urn:opal:%s.%s", tokens[5], tokens[6]);
        // need to rewrite id
        this.id = specialCharCodec.encode(encode(datasetId, name, type,studyId, source, tableType));
      } else {
        source = null;
      }
    }

    public String getId() {
      return id;
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

    public String getStudyId() {
      return studyId;
    }

    public boolean hasStudyId() {
      return !Strings.isNullOrEmpty(studyId);
    }

    public boolean hasSource() {
      return !Strings.isNullOrEmpty(source);
    }

    public String getSource() {
      return source;
    }

    @Override
    public String toString() {
      String tableType = type == Type.Dataschema ? OPAL_HARMONIZATION_TABLE_PREFIX : OPAL_STUDY_TABLE_PREFIX;
      return "[" + datasetId + "," + name + "," + type + ", " + tableType + ", " + studyId + ", " + source + "]";
    }

    public String getTableType() {
      return tableType;
    }
  }
}
