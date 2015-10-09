package org.obiba.mica.file.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.file.AttachmentState;

public class FilePublishedEvent extends PersistablePublishedEvent<AttachmentState> {

  public FilePublishedEvent(AttachmentState state) {
    super(state);
  }
}
