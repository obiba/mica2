package org.obiba.mica.core.domain;

import java.util.Objects;

import org.joda.time.DateTime;
import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractGitPersistable implements Persistable<String>, Timestamped {

  private static final long serialVersionUID = -5039056351334888684L;

  private String id;

  private DateTime createdDate = DateTime.now();

  private DateTime lastModifiedDate;

  @Override
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @JsonIgnore
  @Override
  public boolean isNew() {
    return id == null;
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
    return Objects.equals(id, ((AbstractGitPersistable) obj).id);
  }

  protected com.google.common.base.Objects.ToStringHelper toStringHelper() {
    return com.google.common.base.Objects.toStringHelper(this).omitNullValues().add("id", id);
  }

  @Override
  public final String toString() {
    return toStringHelper().toString();
  }

}
