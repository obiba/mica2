package org.obiba.mica.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class Attachment implements Serializable {

  private static final long serialVersionUID = 7881381748865114007L;

  private String type;

  private LocalizableString description;

  @NotNull
  private String gridFsId;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalizableString getDescription() {
    return description;
  }

  public void setDescription(LocalizableString description) {
    this.description = description;
  }

  public String getGridFsId() {
    return gridFsId;
  }

  public void setGridFsId(String gridFsId) {
    this.gridFsId = gridFsId;
  }
}
