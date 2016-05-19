package org.obiba.mica.project.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.project.domain.Project;

public class ProjectUnpublishedEvent extends PersistablePublishedEvent<Project> {

  public ProjectUnpublishedEvent(Project project) {
    super(project);
  }
}
