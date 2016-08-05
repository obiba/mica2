package org.obiba.mica.core.domain;


import java.util.Map;

public interface ModelAware {
  boolean hasModel();

  void setModel(Map<String, Object> model);

  Map<String, Object> getModel();
}
