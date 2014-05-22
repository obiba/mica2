package org.obiba.mica.service.study.event;

import org.obiba.mica.domain.Study;
import org.obiba.mica.event.PersistableDeletedEvent;

public class StudyDeletedEvent extends PersistableDeletedEvent<Study> {

  public StudyDeletedEvent(Study study) {
    super(study);
  }
}
