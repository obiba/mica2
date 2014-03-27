package org.obiba.mica.domain;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@SuppressWarnings("AssignmentToDateFieldFromParameter")
public abstract class AbstractTimestampedDocument implements Timestamped {

  @NotNull
  @CreatedDate
  private LocalDateTime created = LocalDateTime.now();

  @LastModifiedDate
  private LocalDateTime updated;

  @Override
  @NotNull
  public LocalDateTime getCreated() {
    return created;
  }

  public void setCreated(@NotNull LocalDateTime created) {
    this.created = created;
  }

  @Override
  public LocalDateTime getUpdated() {
    return updated;
  }

  public void setUpdated(@NotNull LocalDateTime updated) {
    this.updated = updated;
  }

}
