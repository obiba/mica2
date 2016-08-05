package org.obiba.mica.core.domain;

import java.util.Map;


public abstract class AbstractModelAware extends AbstractGitPersistable implements ModelAware{

  private Map<String, Object> model;

  public boolean hasModel() {
    return model != null;
  }

  public void setModel(Map<String, Object> model) {
    this.model = model;
  }

  public Map<String, Object> getModel() {
    return model;
  }
}
