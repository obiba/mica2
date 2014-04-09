package org.obiba.mica.event;

import org.obiba.mica.domain.Study;

public class StudyUpdatedEvent extends EntityUpdatedEvent {

  public StudyUpdatedEvent(Study study) {
    super(study);
  }
}
