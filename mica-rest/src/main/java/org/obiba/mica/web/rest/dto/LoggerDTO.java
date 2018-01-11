/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Objects;

import ch.qos.logback.classic.Logger;

public class LoggerDTO {

  private String name;

  private String level;

  public LoggerDTO(Logger logger) {
    name = logger.getName();
    level = logger.getEffectiveLevel().toString();
  }

  @JsonCreator
  public LoggerDTO() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("name", name).add("level", level).toString();
  }
}
