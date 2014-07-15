package org.obiba.mica.domain;

import javax.annotation.Nullable;

public interface AttributeAware {

  void addAttribute(Attribute attribute);

  void removeAttribute(Attribute attribute);

  void removeAllAttributes();

  boolean hasAttribute(String attName, @Nullable String namespace);
}
