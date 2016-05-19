package org.obiba.mica.project.domain;

import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.LocalizedString;

public class ProjectState extends EntityState {

  @Override
  public String pathPrefix() {
    return "projects";
  }

  @NotNull
  private LocalizedString name;

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }
}
