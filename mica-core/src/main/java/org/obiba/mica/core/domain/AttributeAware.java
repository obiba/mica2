package org.obiba.mica.core.domain;

import javax.annotation.Nullable;

import org.obiba.mica.core.domain.Attribute;

public interface AttributeAware {

  void addAttribute(Attribute attribute);

  void removeAttribute(Attribute attribute);

  void removeAllAttributes();

  boolean hasAttribute(String attName, @Nullable String namespace);
}
