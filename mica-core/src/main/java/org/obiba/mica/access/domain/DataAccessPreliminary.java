/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.domain;

import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Document
public class DataAccessPreliminary extends DataAccessEntity {

  private static final long serialVersionUID = 23470546579L;

  @NotNull
  private String parentId;

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public static DataAccessPreliminary.Builder newBuilder() {
    return new DataAccessPreliminary.Builder();
  }

  public static class Builder extends DataAccessEntity.Builder {
    private DataAccessPreliminary preliminary;

    public Builder() {
      request = preliminary = new DataAccessPreliminary();
    }

    public Builder parentId(String value) {
      preliminary.parentId = value;
      return this;
    }
  }

}
