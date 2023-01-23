package org.obiba.mica.access.domain;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class DataAccessAmendment extends DataAccessEntityWithParent {

  private static final long serialVersionUID = 1990378967038884329L;

  public static DataAccessAmendment.Builder newBuilder() {
    return new DataAccessAmendment.Builder();
  }

  public static class Builder extends DataAccessEntity.Builder {
    private DataAccessAmendment amendment;

    public Builder() {
      request = amendment = new DataAccessAmendment();
    }

    public Builder parentId(String value) {
      amendment.parentId= value;
      return this;
    }
  }


}
