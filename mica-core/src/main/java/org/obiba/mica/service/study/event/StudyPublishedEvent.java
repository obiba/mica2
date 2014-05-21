package org.obiba.mica.service.study.event;

import org.obiba.mica.domain.StudyState;
import org.obiba.mica.event.PersistablePublishedEvent;

public class StudyPublishedEvent extends PersistablePublishedEvent<StudyState> {

  public StudyPublishedEvent(StudyState studyState) {
    super(studyState);
  }
}
