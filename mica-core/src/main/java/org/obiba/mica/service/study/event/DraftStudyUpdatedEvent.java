package org.obiba.mica.service.study.event;

import org.obiba.mica.domain.Study;
import org.obiba.mica.event.PersistableUpdatedEvent;

public class DraftStudyUpdatedEvent extends PersistableUpdatedEvent<Study> {

  public DraftStudyUpdatedEvent(Study study) {
    super(study);
  }
}
