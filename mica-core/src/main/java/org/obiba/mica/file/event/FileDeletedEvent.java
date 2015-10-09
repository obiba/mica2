package org.obiba.mica.file.event;

import org.obiba.mica.core.event.PersistableDeletedEvent;
import org.obiba.mica.file.AttachmentState;

public class FileDeletedEvent extends PersistableDeletedEvent<AttachmentState> {

  public FileDeletedEvent(AttachmentState state) {
    super(state);
  }
}
