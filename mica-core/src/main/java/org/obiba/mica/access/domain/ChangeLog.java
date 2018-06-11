package org.obiba.mica.access.domain;

import org.joda.time.DateTime;

import java.io.Serializable;

public abstract class ChangeLog implements Serializable {

  protected String author;

  protected DateTime changedOn;

  public DateTime getChangedOn() {
    return changedOn;
  }

  public String getAuthor() {
    return author;
  }


}
