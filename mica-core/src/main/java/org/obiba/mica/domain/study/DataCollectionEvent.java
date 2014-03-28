package org.obiba.mica.domain.study;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.obiba.mica.domain.Attachment;
import org.obiba.mica.domain.LocalizedString;

public class DataCollectionEvent implements Serializable {

  private static final long serialVersionUID = 6559914069652243954L;

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
    return dataSources == null ? (dataSources = new ArrayList<>()) : dataSources;
  }

  public void setDataSources(List<String> dataSources) {
    this.dataSources = dataSources;
  }

  public List<String> getAdministrativeDatabases() {
    return administrativeDatabases == null ? (administrativeDatabases = new ArrayList<>()) : administrativeDatabases;
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
    return bioSamples == null ? (bioSamples = new ArrayList<>()) : bioSamples;
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
    return attachments == null ? (attachments = new ArrayList<>()) : attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }
}
