package org.obiba.mica.access.domain;

import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Document
public class DataAccessFeasibility extends DataAccessEntity {

  private static final long serialVersionUID = 23403789670546579L;

  @NotNull
  private String parentId;

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public static DataAccessFeasibility.Builder newBuilder() {
    return new DataAccessFeasibility.Builder();
  }

  public static class Builder extends DataAccessEntity.Builder {
    private DataAccessFeasibility feasibility;

    public Builder() {
      request = feasibility = new DataAccessFeasibility();
    }

    public Builder parentId(String value) {
      feasibility.parentId = value;
      return this;
    }
  }

}
