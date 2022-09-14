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

import org.joda.time.DateTime;

public class StatusChange extends ChangeLog {

  private static final long serialVersionUID = -3662401180541149163L;

  private DataAccessEntityStatus from;

  private DataAccessEntityStatus to;

  private String content;

  public DataAccessEntityStatus getFrom() {
    return from;
  }

  public DataAccessEntityStatus getTo() {
    return to;
  }

  public String getContent() {
    return content;
  }

  public static Builder newBuilder(StatusChange source) {
    return new Builder(source);
  }
  public static Builder newBuilder() {
    return newBuilder(null);
  }

  public static class Builder {
    private final StatusChange statusChange;

    private Builder(StatusChange source) {
      statusChange = source == null ?  new StatusChange() : source;
    }

    public Builder previous(DataAccessEntityStatus value) {
      statusChange.from = value;
      return this;
    }

    public Builder current(DataAccessEntityStatus value) {
      statusChange.to = value;
      return this;
    }

    public Builder content(String content) {
      statusChange.content = content;
      return this;
    }

    public Builder author(String value) {
      statusChange.author = value;
      return this;
    }

    public Builder now() {
      statusChange.changedOn = DateTime.now();
      return this;
    }

    public Builder changedOn(DateTime value) {
      statusChange.changedOn = value;
      return this;
    }

    public StatusChange build() {
      return statusChange;
    }
  }
}
