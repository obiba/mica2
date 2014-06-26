/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.domain;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Persistable;

/**
 * Proxy to Opal tables.
 */
public class Dataset implements Persistable<String> {

  private static final long serialVersionUID = -3328963766855899217L;

  private String id;

  @NotNull
  private String name;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return id == null;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {return Objects.hash(id);}

  @Override
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    return Objects.equals(id, ((Dataset) obj).id);
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this).add("id", id).add("name", name).toString();
  }
}