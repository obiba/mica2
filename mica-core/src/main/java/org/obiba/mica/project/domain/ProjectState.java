package org.obiba.mica.project.domain;

import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.LocalizedString;

public class ProjectState extends EntityState {

  @Override
  public String pathPrefix() {
    return "projects";
  }

  private LocalizedString title;

  public LocalizedString getTitle() {
    return title;
  }

  public void setTitle(LocalizedString title) {
    this.title = title;
  }
}
