package org.obiba.mica.domain;

import java.util.Date;

import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@SuppressWarnings("AssignmentToDateFieldFromParameter")
public abstract class AbstractTimestampedDocument implements Timestamped {

  @NotNull
  @CreatedDate
  private Date created = new Date();

  @LastModifiedDate
  private Date updated;

  @Override
  @NotNull
  public Date getCreated() {
    return created;
  }

  public void setCreated(@NotNull Date created) {
    this.created = created;
  }

  @Override
  public Date getUpdated() {
    return updated;
  }

  public void setUpdated(@NotNull Date updated) {
    this.updated = updated;
  }

}
