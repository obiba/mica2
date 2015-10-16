package org.obiba.mica.core.domain;

import java.util.Objects;

import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

public abstract class AbstractAuditableDocument implements Auditable<String, String>, Timestamped {

  private static final long serialVersionUID = -5039056351334888684L;

  @Id
  private String id;

  @Version
  private Long version;

  private String createdBy;

  @CreatedDate
  private DateTime createdDate = DateTime.now();

  private String lastModifiedBy;

  @LastModifiedDate
  private DateTime lastModifiedDate;

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @JsonIgnore
  @Override
  public boolean isNew() {
    return id == null;
  }

  @Override
  public String getCreatedBy() {
    return createdBy;
  }

  @Override
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public DateTime getCreatedDate() {
    return createdDate;
  }

  @Override
  public void setCreatedDate(DateTime createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  @Override
  public DateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @Override
  public void setLastModifiedDate(DateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((AbstractAuditableDocument) obj).id);
  }

  protected MoreObjects.ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this).omitNullValues().add("id", id).add("version", version);
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

}
