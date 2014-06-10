package org.obiba.mica.study.event;

import org.obiba.mica.event.PersistableUpdatedEvent;
import org.obiba.mica.study.domain.Study;

public class DraftStudyUpdatedEvent extends PersistableUpdatedEvent<Study> {

  public DraftStudyUpdatedEvent(Study study) {
    super(study);
  }
}
