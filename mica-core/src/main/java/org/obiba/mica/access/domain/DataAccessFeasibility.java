package org.obiba.mica.access.domain;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class DataAccessFeasibility extends DataAccessEntityWithParent {

  private static final long serialVersionUID = 23403789670546579L;

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
