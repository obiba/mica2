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
public class DataAccessAgreement extends DataAccessEntity {

  private static final long serialVersionUID = 56756037768797054L;

  @NotNull
  private String parentId;

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public static DataAccessAgreement.Builder newBuilder() {
    return new DataAccessAgreement.Builder();
  }

  public static class Builder extends DataAccessEntity.Builder {
    private DataAccessAgreement agreement;

    public Builder() {
      request = agreement = new DataAccessAgreement();
    }

    public Builder parentId(String value) {
      agreement.parentId = value;
      return this;
    }
  }

}
