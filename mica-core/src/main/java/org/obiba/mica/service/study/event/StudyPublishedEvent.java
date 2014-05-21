package org.obiba.mica.service.study.event;

import org.obiba.mica.domain.Study;
import org.obiba.mica.event.PersistablePublishedEvent;

public class StudyPublishedEvent extends PersistablePublishedEvent<Study> {

  public StudyPublishedEvent(Study study) {
    super(study);
  }
}
