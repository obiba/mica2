/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.magma.Variable;
import org.obiba.magma.support.VariableNature;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;
import org.obiba.mica.core.domain.Indexable;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.domain.NetworkTable;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.domain.StudyTable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;


public class DatasetVariable implements Indexable, AttributeAware {

  private static final long serialVersionUID = -141834508275072637L;

  public static final String MAPPING_NAME = "Variable";

  public static final String HMAPPING_NAME = "H" + MAPPING_NAME;

  private static final String ID_SEPARATOR = ":";
  public static final String OPAL_STUDY_TABLE_PREFIX = "Study";
  public static final String OPAL_NETWORK_TABLE_PREFIX = "Network";

  public enum Type {
    Study,       // variable extracted from a study dataset
    Dataschema,  // variable extracted from a harmonization dataset
    Harmonized   // variable that implements a Datashema variable
  }

  public enum OpalTableType {
    Study,
    Network
  }

  @NotNull
  private String datasetId;

  private String networkId;

  private List<String> networkTableIds = Lists.newArrayList();

  private List<String> studyIds = Lists.newArrayList();

  private List<String> dceIds = Lists.newArrayList();

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

  public DatasetVariable() {}

  public DatasetVariable(StudyDataset dataset, Variable variable) {
    this(dataset, Type.Study, variable);
    studyIds = Lists.newArrayList(dataset.getStudyTable().getStudyId());
    dceIds = Lists.newArrayList(dataset.getStudyTable().getDataCollectionEventUId());
  }

  public DatasetVariable(HarmonizationDataset dataset, Variable variable) {
    this(dataset, Type.Dataschema, variable);

    dataset.getStudyTables().forEach(table -> {
      if(!studyIds.contains(table.getStudyId())) studyIds.add(table.getStudyId());
    });

    dataset.getNetworkTables().forEach(table -> {
      if(!networkTableIds.contains(table.getNetworkId())) networkTableIds.add(table.getNetworkId());
    });

    networkId = dataset.getNetworkId();

    dataset.getStudyTables().forEach(table -> {
      if(!dceIds.contains(table.getDataCollectionEventUId())) dceIds.add(table.getDataCollectionEventUId());
    });
  }

  public DatasetVariable(HarmonizationDataset dataset, Variable variable, OpalTable opalTable) {
    this(dataset, Type.Harmonized, variable);

    if(opalTable instanceof StudyTable) {
      studyIds = Lists.newArrayList(((StudyTable) opalTable).getStudyId());
      dceIds = Lists.newArrayList(((StudyTable) opalTable).getDataCollectionEventUId());
      opalTableType = OpalTableType.Study;
    } else {
      networkTableIds = Lists.newArrayList(((NetworkTable) opalTable).getNetworkId());
      opalTableType = OpalTableType.Network;
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
    String id = datasetId + ID_SEPARATOR + name + ID_SEPARATOR + variableType;

    if(Type.Harmonized == variableType) {
      String entityId = opalTableType == OpalTableType.Study ? studyIds.get(0) : networkTableIds.get(0);
      String tableType = opalTableType == OpalTableType.Study ? OPAL_STUDY_TABLE_PREFIX : OPAL_NETWORK_TABLE_PREFIX;

      id = id + ID_SEPARATOR + tableType + ID_SEPARATOR + entityId + ID_SEPARATOR + project + ID_SEPARATOR + table;
    }

    return id;
  }

  public void setId(String id) {
    IdResolver resolver = IdResolver.from(id);

    variableType = resolver.getType();
    datasetId = resolver.getDatasetId();
    name = resolver.getName();
    opalTableType = resolver.hasStudyId() ? OpalTableType.Study : OpalTableType.Network;

    if(resolver.hasStudyId()) studyIds = Lists.newArrayList(resolver.getStudyId());
    if(resolver.hasNetworkId()) networkTableIds = Lists.newArrayList(resolver.getNetworkId());
    if(resolver.hasProject()) project = resolver.getProject();
    if(resolver.hasTable()) table = resolver.getTable();
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

  public List<String> getStudyIds() {
    return studyIds;
  }

  public List<String> getNetworkTableIds() {
    return networkTableIds;
  }

  public List<String> getDceIds() {
    return dceIds;
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

  public String getNetworkId() {
    return networkId;
  }

  public void setNetworkId(String networkId) {
    this.networkId = networkId;
  }

  public static class IdResolver {

    private final String id;

    private final Type type;

    private final String datasetId;

    private final String name;

    private final String studyId;

    private final String networkId;

    private final String project;

    private final String table;

    public static IdResolver from(String id) {
      return new IdResolver(id);
    }

    public static IdResolver from(String datasetId, String variableName, Type variableType) {
      return from(encode(datasetId, variableName, variableType, null));
    }

    public static String encode(String datasetId, String variableName, Type variableType, String studyId,
      String project, String table, String networkId) {
      String id = datasetId + ID_SEPARATOR + variableName + ID_SEPARATOR + variableType;

      if(Type.Harmonized == variableType) {
        String tableType;
        String entityId;

        if(studyId != null) {
          tableType = OPAL_STUDY_TABLE_PREFIX;
          entityId = studyId;
        } else {
          tableType = OPAL_NETWORK_TABLE_PREFIX;
          entityId = networkId;
        }

        id = id + ID_SEPARATOR + tableType + ID_SEPARATOR + entityId + ID_SEPARATOR + project + ID_SEPARATOR + table;
      }

      return id;
    }

    public static String encode(String datasetId, String variableName, Type variableType, OpalTable opalTable) {
      return opalTable == null
        ? encode(datasetId, variableName, variableType, null, null, null, null)
        : opalTable instanceof StudyTable ? encode(datasetId, variableName, variableType, ((StudyTable) opalTable).getStudyId(), opalTable.getProject(),
          opalTable.getTable(), null) : encode(datasetId, variableName, variableType, null, opalTable.getProject(),
          opalTable.getTable(), ((NetworkTable) opalTable).getNetworkId());
    }

    private IdResolver(String id) {
      if(id == null) throw new IllegalArgumentException("Dataset variable cannot be null");
      this.id = id;

      String[] tokens = id.split(ID_SEPARATOR);
      if(tokens.length < 3) throw new IllegalArgumentException("Not a valid dataset variable ID: " + id);

      datasetId = tokens[0];
      name = tokens[1];
      type = Type.valueOf(tokens[2]);

      String tableType = tokens.length > 3 ? tokens[3] : null;

      if(OPAL_NETWORK_TABLE_PREFIX.equals(tableType)) {
        networkId = tokens.length > 4 ? tokens[4] : null;
        studyId = null;
      } else {
        networkId = null;
        studyId = tokens.length > 4 ? tokens[4] : null;
      }

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

    public String getNetworkId() {
      return networkId;
    }

    public boolean hasNetworkId() {
      return !Strings.isNullOrEmpty(networkId);
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
      String tableType = networkId == null ? OPAL_NETWORK_TABLE_PREFIX : OPAL_STUDY_TABLE_PREFIX;
      String entityId = networkId == null ? studyId : networkId;

      return "[" + datasetId + "," + name + "," + type + ", " + tableType + ", " + entityId + ", " + project + ", " + table + "]";
    }
  }
}
