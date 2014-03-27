package org.obiba.mica.domain.study;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.LocalizableString;

public class DataCollectionEvent implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

  @NotNull
  private LocalizableString name;

  private LocalizableString description;

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

  private LocalizableString otherDataSources;

  private List<String> bioSamples;

  private LocalizableString tissueTypes;

  private LocalizableString otherBioSamples;

  private List<Attachment> attachments;

  public LocalizableString getName() {
    return name;
  }

  public void setName(LocalizableString name) {
    this.name = name;
  }

  public LocalizableString getDescription() {
    return description;
  }

  public void setDescription(LocalizableString description) {
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

  public void setDataSources(List<String> dataSources) {
    this.dataSources = dataSources;
  }

  public List<String> getAdministrativeDatabases() {
    return administrativeDatabases;
  }

  public void setAdministrativeDatabases(List<String> administrativeDatabases) {
    this.administrativeDatabases = administrativeDatabases;
  }

  public LocalizableString getOtherDataSources() {
    return otherDataSources;
  }

  public void setOtherDataSources(LocalizableString otherDataSources) {
    this.otherDataSources = otherDataSources;
  }

  public List<String> getBioSamples() {
    return bioSamples;
  }

  public void setBioSamples(List<String> bioSamples) {
    this.bioSamples = bioSamples;
  }

  public LocalizableString getTissueTypes() {
    return tissueTypes;
  }

  public void setTissueTypes(LocalizableString tissueTypes) {
    this.tissueTypes = tissueTypes;
  }

  public LocalizableString getOtherBioSamples() {
    return otherBioSamples;
  }

  public void setOtherBioSamples(LocalizableString otherBioSamples) {
    this.otherBioSamples = otherBioSamples;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }
}
