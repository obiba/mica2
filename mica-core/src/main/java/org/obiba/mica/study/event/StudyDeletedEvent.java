package org.obiba.mica.study.event;

import org.obiba.mica.core.event.PersistableDeletedEvent;
import org.obiba.mica.study.domain.Study;

public class StudyDeletedEvent extends PersistableDeletedEvent<Study> {

  public StudyDeletedEvent(Study study) {
    super(study);
  }
}
