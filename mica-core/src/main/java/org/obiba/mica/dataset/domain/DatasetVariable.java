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
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.obiba.magma.Variable;
import org.obiba.magma.support.VariableNature;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.spi.search.Indexable;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class DatasetVariable implements Indexable, AttributeAware {

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

  private boolean repeatable;

  private String occurrenceGroup;

  private List<DatasetCategory> categories;

  private Attributes attributes;

  private String nature;

  private int index;

  private String project;

  private String table;

  private OpalTableType opalTableType;

  @NotNull
  private LocalizedString datasetAcronym;

  @NotNull
  private LocalizedString datasetName;

  private String containerId;

  private int populationWeight;

  private int dataCollectionEventWeight;

  private Set<String> sets;

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

  public DatasetVariable(HarmonizationDataset dataset, Variable variable, OpalTable opalTable) {
    this(dataset, Type.Harmonized, variable);

    if (opalTable instanceof BaseStudyTable) {
      studyId = ((BaseStudyTable) opalTable).getStudyId();
      populationId = ((BaseStudyTable) opalTable).getPopulationUId();
      dceId = ((BaseStudyTable) opalTable).getDataCollectionEventUId();
      setContainerId(studyId);
      opalTableType = opalTable instanceof StudyTable ? OpalTableType.Study : OpalTableType.Harmonization;
    }

    project = opalTable.getProject();
    table = opalTable.getTable();
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
    String id = datasetId + ID_SEPARATOR + name + ID_SEPARATOR + variableType;

    if (Type.Harmonized == variableType) {
      String entityId = studyId;
      String tableType = opalTableType == OpalTableType.Study ? OPAL_STUDY_TABLE_PREFIX : OPAL_HARMONIZATION_TABLE_PREFIX;
      id = id + ID_SEPARATOR + tableType + ID_SEPARATOR + entityId + ID_SEPARATOR + project + ID_SEPARATOR + table;
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
    if (resolver.hasProject()) project = resolver.getProject();
    if (resolver.hasTable()) table = resolver.getTable();
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

  public boolean hasCategories() {
    return categories != null && !categories.isEmpty();
  }

  public List<DatasetCategory> getCategories() {
    return categories;
  }

  public DatasetCategory getCategory(String name) {
    if (!hasCategories()) return null;

    for (DatasetCategory category : categories) {
      if (category.getName().equals(name)) return category;
    }

    return null;
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

  protected com.google.common.base.Objects.ToStringHelper toStringHelper() {
    return com.google.common.base.Objects.toStringHelper(this).omitNullValues().add("id", getId());
  }

  @Override
  public final String toString() {
    return toStringHelper().toString();
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

  public static class IdResolver {

    private final String id;

    private final Type type;

    private final String datasetId;

    private final String name;

    private final String studyId;

    private final String project;

    private final String table;

    private final String tableType;

    public static IdResolver from(String id) {
      return new IdResolver(id);
    }

    public static IdResolver from(String datasetId, String variableName, Type variableType) {
      return from(encode(datasetId, variableName, variableType, null));
    }

    public static String encode(String datasetId, String variableName, Type variableType, String studyId,
                                String project, String table, String tableType) {
      String id = datasetId + ID_SEPARATOR + variableName + ID_SEPARATOR + variableType;

      String entityId;

      if (Type.Harmonized == variableType) {
        entityId = studyId;
        id = id + ID_SEPARATOR + tableType + ID_SEPARATOR + entityId + ID_SEPARATOR + project + ID_SEPARATOR + table;
      }

      return id;
    }

    public static String encode(String datasetId, String variableName, Type variableType, OpalTable opalTable) {
      String tableType = opalTable instanceof StudyTable ? OPAL_STUDY_TABLE_PREFIX : OPAL_HARMONIZATION_TABLE_PREFIX;
      BaseStudyTable studyTable = (BaseStudyTable)opalTable;
      return studyTable == null
          ? encode(datasetId, variableName, variableType, null, null, null, null)
          : encode(datasetId,
            variableName,
            variableType,
            studyTable.getStudyId(),
            opalTable.getProject(),
            opalTable.getTable(),
            tableType);
    }

    private IdResolver(String id) {
      if (Strings.isNullOrEmpty(id)) throw new IllegalArgumentException("Dataset variable cannot be null or empty");
      this.id = id;

      String[] tokens = id.split(ID_SEPARATOR);
      if (tokens.length < 3) throw new IllegalArgumentException("Not a valid dataset variable ID: " + id);

      datasetId = tokens[0];
      name = tokens[1];
      type = Type.valueOf(tokens[2]);

      tableType = tokens.length > 3 ? tokens[3] : null;
      studyId = tokens.length > 4 ? tokens[4] : null;

      project = tokens.length > 5 ? tokens[5] : null;
      table = tokens.length > 6 ? tokens[6] : null;
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

    public String getProject() {
      return project;
    }

    public boolean hasProject() {
      return !Strings.isNullOrEmpty(project);
    }

    public String getTable() {
      return table;
    }

    public boolean hasTable() {
      return !Strings.isNullOrEmpty(table);
    }

    @Override
    public String toString() {
      String tableType = type == Type.Dataschema ? OPAL_HARMONIZATION_TABLE_PREFIX : OPAL_STUDY_TABLE_PREFIX;
      return "[" + datasetId + "," + name + "," + type + ", " + tableType + ", " + studyId + ", " + project + ", " + table + "]";
    }

    public String getTableType() {
      return tableType;
    }
  }
}
