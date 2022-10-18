package org.obiba.mica.access.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class ChangeLog implements Serializable {

  protected String author;

  protected LocalDateTime changedOn;

  public LocalDateTime getChangedOn() {
    return changedOn;
  }

  public String getAuthor() {
    return author;
  }


}
