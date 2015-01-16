package org.obiba.mica.micaConfig;

import org.obiba.mica.core.domain.LocalizedString;

public class AggregationInfo {
  private String id;

  private LocalizedString title;

  public LocalizedString getTitle() {
    return title;
  }

  public void setTitle(LocalizedString title) {
    this.title = title;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
