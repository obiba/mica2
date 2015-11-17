package org.obiba.mica.study.event;

import org.obiba.mica.core.domain.PublishCascadingScope;
import org.obiba.mica.core.event.PersistableCascadingPublishedEvent;
import org.obiba.mica.study.domain.Study;

public class StudyPublishedEvent extends PersistableCascadingPublishedEvent<Study> {

  private final String publisher;

  public StudyPublishedEvent(Study study, String publisher) {
    this(study, publisher, PublishCascadingScope.NONE);
  }

  public StudyPublishedEvent(Study study, String publisher, PublishCascadingScope cascadingScope) {
    super(study, cascadingScope);
    this.publisher = publisher;
  }

  public String getPublisher() {
    return publisher;
  }
}
