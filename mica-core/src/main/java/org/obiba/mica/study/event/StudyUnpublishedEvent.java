package org.obiba.mica.study.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.study.domain.Study;

public class StudyUnpublishedEvent extends PersistablePublishedEvent<Study> {

  public StudyUnpublishedEvent(Study study) {
    super(study);
  }
}
