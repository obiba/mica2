package org.obiba.mica.study.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.study.domain.Study;

public class StudyPublishedEvent extends PersistablePublishedEvent<Study> {

  private final String publisher;

  public StudyPublishedEvent(Study study, String publisher) {
    super(study);
    this.publisher = publisher;
  }

  public String getPublisher() {
    return publisher;
  }
}
