package org.obiba.mica.core.event;

import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;

public class IndexPersistablesEvent<TPersistable extends Persistable<?>> {
  private List<String> ids;

  public IndexPersistablesEvent() {
  }

  public IndexPersistablesEvent(List<String> ids) {
    this.ids = ids;
  }

  public List<String> getIds() {
    return ids == null ? new ArrayList<>() : ids;
  }

}
