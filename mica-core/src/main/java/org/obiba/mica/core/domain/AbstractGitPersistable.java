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

import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.obiba.mica.spi.search.Identified;
import org.springframework.data.domain.Persistable;

public abstract class AbstractGitPersistable implements GitPersistable, Persistable<String>, Identified {

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
    return Strings.isNullOrEmpty(id);
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

  protected MoreObjects.ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this).omitNullValues().add("id", id);
  }

  @Override
  public final String toString() {
    return toStringHelper().toString();
  }
}
