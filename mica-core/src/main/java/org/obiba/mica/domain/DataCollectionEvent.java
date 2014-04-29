package org.obiba.mica.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;

public class DataCollectionEvent implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  private String id = new ObjectId().toString();

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  @NotNull
  private Integer startYear;

  @Min(1)
  @Max(12)
  private Integer startMonth;

  private Integer endYear;

  @Min(1)
  @Max(12)
  private Integer endMonth;

  private List<String> dataSources;

  private List<String> administrativeDatabases;

  private LocalizedString otherDataSources;

  private List<String> bioSamples;

  private LocalizedString tissueTypes;

  private LocalizedString otherBioSamples;

  private List<Attachment> attachments;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

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

  public Integer getStartYear() {
    return startYear;
  }

  public void setStartYear(Integer startYear) {
    this.startYear = startYear;
  }

  public Integer getStartMonth() {
    return startMonth;
  }

  public void setStartMonth(Integer startMonth) {
    this.startMonth = startMonth;
  }

  public Integer getEndYear() {
    return endYear;
  }

  public void setEndYear(Integer endYear) {
    this.endYear = endYear;
  }

  public Integer getEndMonth() {
    return endMonth;
  }

  public void setEndMonth(Integer endMonth) {
    this.endMonth = endMonth;
  }

  public List<String> getDataSources() {
    return dataSources;
  }

  public void addDataSource(@NotNull String datasource) {
    if (dataSources == null) dataSources = new ArrayList<>();
    dataSources.add(datasource);
  }

  public void setDataSources(List<String> dataSources) {
    this.dataSources = dataSources;
  }

  public List<String> getAdministrativeDatabases() {
    return administrativeDatabases;
  }

  public void addAdministrativeDatabases(@NotNull String database) {
    if (administrativeDatabases == null) administrativeDatabases = new ArrayList<>();
    administrativeDatabases.add(database);
  }

  public void setAdministrativeDatabases(List<String> administrativeDatabases) {
    this.administrativeDatabases = administrativeDatabases;
  }

  public LocalizedString getOtherDataSources() {
    return otherDataSources;
  }

  public void setOtherDataSources(LocalizedString otherDataSources) {
    this.otherDataSources = otherDataSources;
  }

  public List<String> getBioSamples() {
    return bioSamples;
  }

  public void addBioSample(@NotNull String bioSample) {
    if (bioSamples == null) bioSamples = new ArrayList<>();
    bioSamples.add(bioSample);
  }

  public void setBioSamples(List<String> bioSamples) {
    this.bioSamples = bioSamples;
  }

  public LocalizedString getTissueTypes() {
    return tissueTypes;
  }

  public void setTissueTypes(LocalizedString tissueTypes) {
    this.tissueTypes = tissueTypes;
  }

  public LocalizedString getOtherBioSamples() {
    return otherBioSamples;
  }

  public void setOtherBioSamples(LocalizedString otherBioSamples) {
    this.otherBioSamples = otherBioSamples;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void addAttachment(@NotNull Attachment attachment) {
    if (attachments == null) attachments = new ArrayList<>();
    attachments.add(attachment);
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DataCollectionEvent other = (DataCollectionEvent) obj;
    return Objects.equals(id, other.id);
  }
}
