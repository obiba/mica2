package org.obiba.mica.file.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.file.AttachmentState;

public class FileUpdatedEvent extends PersistablePublishedEvent<AttachmentState> {

  public FileUpdatedEvent(AttachmentState state) {
    super(state);
  }
}
