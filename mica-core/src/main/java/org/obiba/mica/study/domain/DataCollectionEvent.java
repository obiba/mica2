package org.obiba.mica.study.domain;

import java.io.Serializable;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.obiba.mica.domain.AbstractAttributeAware;
import org.obiba.mica.domain.LocalizedString;
import org.obiba.mica.file.Attachment;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

public class DataCollectionEvent extends AbstractAttributeAware
    implements Serializable, Persistable<String>, Comparable<DataCollectionEvent> {

  private static final long serialVersionUID = 6559914069652243954L;

  private String id = new ObjectId().toString();

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  private YearMonth start;

  private YearMonth end;

  private List<String> dataSources;

  private List<String> administrativeDatabases;

  private LocalizedString otherDataSources;

  private List<String> bioSamples;

  private LocalizedString tissueTypes;

  private LocalizedString otherBioSamples;

  private List<Attachment> attachments;

  @Override
  public String getId() {
    return id;
  }

  @JsonIgnore
  @Override
  public boolean isNew() {
    return false;
  }

  public void setId(String id) {
    if(!Strings.isNullOrEmpty(id)) this.id = id;
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

  public YearMonth getStart() {
    return start;
  }

  public void setStart(YearMonth start) {
    this.start = start;
  }

  public void setStart(int year, @Nullable Integer month) {
    start = YearMonth.of(year, month == null ? Month.JANUARY.getValue() : month);
  }

  public YearMonth getEnd() {
    return end;
  }

  public void setEnd(YearMonth end) {
    this.end = end;
  }

  public void setEnd(int year, @Nullable Integer month) {
    end = YearMonth.of(year, month == null ? Month.DECEMBER.getValue() : month);
  }

  public List<String> getDataSources() {
    return dataSources;
  }

  public void addDataSource(@NotNull String datasource) {
    if(dataSources == null) dataSources = new ArrayList<>();
    dataSources.add(datasource);
  }

  public void setDataSources(List<String> dataSources) {
    this.dataSources = dataSources;
  }

  public List<String> getAdministrativeDatabases() {
    return administrativeDatabases;
  }

  public void addAdministrativeDatabases(@NotNull String database) {
    if(administrativeDatabases == null) administrativeDatabases = new ArrayList<>();
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
    if(bioSamples == null) bioSamples = new ArrayList<>();
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
    if(attachments == null) attachments = new ArrayList<>();
    attachments.add(attachment);
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((DataCollectionEvent) obj).id);
  }

  @Override
  public int compareTo(@NotNull DataCollectionEvent dce) {
    ComparisonChain chain = ComparisonChain.start();

    if(start != null && dce.start != null) {
      chain = chain.compare(start, dce.start);
    } else if(start != dce.start) {
      return start == null ? 1 : -1;
    }

    if(end != null && dce.end != null) {
      chain = chain.compare(end, dce.end);
    } else if(end != dce.end) {
      return end == null ? 1 : -1;
    }

    return chain.compare(id, dce.id).result();
  }
}
