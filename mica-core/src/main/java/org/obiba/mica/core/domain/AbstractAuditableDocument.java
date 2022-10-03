/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.obiba.mica.spi.search.Identified;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Auditable;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

public abstract class AbstractAuditableDocument implements Auditable<String, String, LocalDateTime>, Timestamped, Identified {

  private static final long serialVersionUID = 2L;

  @Id
  private String id;

  @Version
  private Long version;

  private String createdBy;

  @CreatedDate
  private LocalDateTime createdDate = LocalDateTime.now();

  private String lastModifiedBy;

  @LastModifiedDate
  private LocalDateTime lastModifiedDate;

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
    return Strings.isNullOrEmpty(id);
  }

  @Override
  public Optional<String> getCreatedBy() {
    return Optional.ofNullable(createdBy);
  }

  @JsonGetter("createdBy")
  public String getNullableCreatedBy() {
    return createdBy;
  }

  @Override
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public Optional<LocalDateTime> getCreatedDate() {
    return Optional.ofNullable(createdDate);
  }

  @JsonGetter("createdDate")
  public LocalDateTime getNullableCreatedDate() {
    return createdDate;
  }

  @Override
  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public boolean hasLastModifiedBy() {
    return !Strings.isNullOrEmpty(lastModifiedBy);
  }

  @Override
  public Optional<String> getLastModifiedBy() {
    return Optional.ofNullable(lastModifiedBy);
  }

  @JsonGetter("lastModifiedBy")
  public String getNullableLastModifiedBy() {
    return lastModifiedBy;
  }

  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  @Override
  public Optional<LocalDateTime> getLastModifiedDate() {
    return Optional.ofNullable(lastModifiedDate);
  }

  @JsonGetter("lastModifiedDate")
  public LocalDateTime getNullableLastModifiedDate() {
    return lastModifiedDate;
  }

  @Override
  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
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
