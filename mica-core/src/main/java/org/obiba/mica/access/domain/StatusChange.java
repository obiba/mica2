/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.domain;

import java.io.Serializable;

import org.joda.time.DateTime;

public class StatusChange implements Serializable {

  private static final long serialVersionUID = -3662401180541149163L;

  private DataAccessEntityStatus from;

  private DataAccessEntityStatus to;

  private String author;

  private DateTime changedOn;

  public DataAccessEntityStatus getFrom() {
    return from;
  }

  public void setFrom(DataAccessEntityStatus value) {
    from = value;
  }

  public DataAccessEntityStatus getTo() {
    return to;
  }

  public void setTo(DataAccessEntityStatus value) {
    to = value;
  }

  public DateTime getChangedOn() {
    return changedOn;
  }

  public void setChangedOn(DateTime value) {
    changedOn = value;
  }

  public static Builder newBuilder(StatusChange source) {
    return new Builder(source);
  }
  public static Builder newBuilder() {
    return newBuilder(null);
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public static class Builder {
    private final StatusChange statusChange;

    private Builder(StatusChange source) {
      statusChange = source == null ?  new StatusChange() : source;
    }

    public Builder previous(DataAccessEntityStatus value) {
      statusChange.setFrom(value);
      return this;
    }

    public Builder current(DataAccessEntityStatus value) {
      statusChange.setTo(value);
      return this;
    }

    public Builder author(String value) {
      statusChange.setAuthor(value);
      return this;
    }

    public Builder now() {
      statusChange.setChangedOn(DateTime.now());
      return this;
    }

    public Builder changedOn(DateTime value) {
      statusChange.setChangedOn(value);
      return this;
    }

    public StatusChange build() {
      return statusChange;
    }
  }
}
