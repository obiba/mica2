/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Authorization implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  private boolean authorized;

  private String authorizer;

  private Date date;

  public boolean isAuthorized() {
    return authorized;
  }

  public void setAuthorized(boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizer() {
    return authorizer;
  }

  public void setAuthorizer(String authorizer) {
    this.authorizer = authorizer;
  }

  public Date getDate() {
    return date;
  }

  @JsonProperty
  public void setDate(Date date) {
    this.date = date;
  }

  @JsonIgnore
  public void setDate(LocalDate date) {
    this.date = new Date();
    this.date.setTime(date.toEpochDay());
  }

  public LocalDate asLocalDate() {
    return LocalDate.ofEpochDay(getDate().getTime());
  }
}
