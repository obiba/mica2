package org.obiba.mica.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

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
    return "LoggerDTO{" +
        "name='" + name + '\'' +
        ", level='" + level + '\'' +
        '}';
  }
}
