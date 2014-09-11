package org.obiba.mica.study.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.study.domain.Study;

public class StudyPublishedEvent extends PersistablePublishedEvent<Study> {

  public StudyPublishedEvent(Study study) {
    super(study);
  }
}
