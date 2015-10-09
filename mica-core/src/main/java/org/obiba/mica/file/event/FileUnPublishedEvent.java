package org.obiba.mica.file.event;

import org.obiba.mica.core.event.PersistablePublishedEvent;
import org.obiba.mica.file.AttachmentState;

public class FileUnPublishedEvent extends PersistablePublishedEvent<AttachmentState> {

  public FileUnPublishedEvent(AttachmentState state) {
    super(state);
  }
}
