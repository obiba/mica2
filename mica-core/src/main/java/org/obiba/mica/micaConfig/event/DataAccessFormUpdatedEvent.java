package org.obiba.mica.micaConfig.event;

import org.obiba.mica.micaConfig.domain.DataAccessForm;

public class DataAccessFormUpdatedEvent {

  private final DataAccessForm form;

  public DataAccessFormUpdatedEvent(DataAccessForm form) {
    this.form = form;
  }

  public DataAccessForm getForm() {
    return form;
  }
}
