package org.obiba.mica.study.domain;

import java.io.Serializable;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AbstractAttributeAware;
import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.study.date.PersistableYearMonth;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.DBRef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DataCollectionEvent extends AbstractAttributeAware
    implements Serializable, Persistable<String>, Comparable<DataCollectionEvent>, AttachmentAware {

  private static final long serialVersionUID = 6559914069652243954L;

  private String id;

  @NotNull
  private LocalizedString name;

  private LocalizedString description;

  private PersistableYearMonth start;

  private PersistableYearMonth end;

  private List<String> dataSources;

  private List<String> administrativeDatabases;

  private LocalizedString otherDataSources;

  private List<String> bioSamples;

  private LocalizedString tissueTypes;

  private LocalizedString otherBioSamples;

  @DBRef
  private List<Attachment> attachments = Lists.newArrayList();

  private Iterable<Attachment> removedAttachments = Lists.newArrayList();

  @Override
  public String getId() {
    return id;
  }

  @JsonIgnore
  @Override
  public boolean isNew() {
    return Strings.isNullOrEmpty(id);
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

  public PersistableYearMonth getStart() {
    return start;
  }

  public void setStart(PersistableYearMonth start)
  {
    this.start = start;
  }

  public void setStart(int year, @Nullable Integer month) {
    start = PersistableYearMonth.of(year, month == null ? Month.JANUARY.getValue() : month);
  }

  public PersistableYearMonth getEnd() {
    return end;
  }

  public void setEnd(PersistableYearMonth end) {
    this.end = end;
  }

  public void setEnd(int year, @Nullable Integer month) {
    end = PersistableYearMonth.of(year, month == null ? Month.DECEMBER.getValue() : month);
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

  @Override
  public boolean hasAttachments() {
    return attachments != null && !attachments.isEmpty();
  }

  @Override
  public List<Attachment> getAttachments() {
    return attachments;
  }

  @Override
  public void addAttachment(@NotNull Attachment attachment) {
    attachments.add(attachment);
  }

  @Override
  public void setAttachments(@NotNull List<Attachment> attachments) {
    if (attachments == null) attachments = Lists.newArrayList();
    removedAttachments = Sets.difference(Sets.newHashSet(this.attachments), Sets.newHashSet(attachments));
    this.attachments = attachments;
  }

  @Override
  public List<Attachment> removedAttachments() {
    return Lists.newArrayList(removedAttachments);
  }

  @Override
  public Iterable<Attachment> getAllAttachments() {
    return this.attachments;
  }

  @Override
  public Attachment findAttachmentById(String attachmentId) {
    return this.attachments.stream().filter(a -> a.getId().equals(attachmentId)).findFirst().orElse(null);
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
